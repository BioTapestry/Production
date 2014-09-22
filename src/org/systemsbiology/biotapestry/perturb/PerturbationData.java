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
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.qpcr.QpcrLegacyPublicExposed;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.BoundedDoubMinMax;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.Splitter;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UniqueLabeller;


/****************************************************************************
**
** This holds a fairly flat description of perturbations
*/

public class PerturbationData implements Cloneable, SourceSrc {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  //
  // Collapse modes:
  //
  
  public final static int NO_COLLAPSE             = 0;
  public final static int DISTINCT_PLUS_MINUS     = 1;  
  public final static int FULL_COLLAPSE_DROP_SIGN = 2; 
  public final static int FULL_COLLAPSE_KEEP_SIGN = 3;  
  
  
  //
  // Legacy data possibilities:
  //
  
  public final static int NO_LEGACY_DATA          = 0x00;
  public final static int HAVE_LEGACY_PERT        = 0x01;
  public final static int HAVE_LEGACY_TIME_SPAN   = 0x02;
  public final static int HAVE_LEGACY_NULL_REGION = 0x04;  
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  final static String SN_SET_XML_TAG_  = "serNumSet";  
  
  
  //
  // Collapse modes. Hauled over from old QPCRData.  Still needed at this
  // level of complexity?
  //
  
  private final static int NO_LINK_   = 0x00;  
  private final static int PLUS_      = 0x01;
  private final static int MINUS_     = 0x02;  
  private final static int UNDEFINED_ = 0x04; 

  private final static int P_AND_M_  = PLUS_ | MINUS_; 
  private final static int P_AND_U_  = PLUS_ | UNDEFINED_;
  private final static int M_AND_U_  = MINUS_ | UNDEFINED_; 
  private final static int ALL_VALS_ = PLUS_ | MINUS_ | UNDEFINED_;
  
  private final static int P_AND_M_DISTINCT_PM_  = P_AND_M_; 
  private final static int P_AND_U_DISTINCT_PM_  = PLUS_; 
  private final static int M_AND_U_DISTINCT_PM_  = MINUS_; 
  private final static int ALL_VALS_DISTINCT_PM_ = P_AND_M_;  
  
  private final static int P_AND_M_FULL_COLLAPSE_DROP_SIGN_  = UNDEFINED_; 
  private final static int P_AND_U_FULL_COLLAPSE_DROP_SIGN_  = UNDEFINED_; 
  private final static int M_AND_U_FULL_COLLAPSE_DROP_SIGN_  = UNDEFINED_; 
  private final static int ALL_VALS_FULL_COLLAPSE_DROP_SIGN_ = UNDEFINED_;  

  private final static int P_AND_M_FULL_COLLAPSE_KEEP_SIGN_  = UNDEFINED_; 
  private final static int P_AND_U_FULL_COLLAPSE_KEEP_SIGN_  = PLUS_; 
  private final static int M_AND_U_FULL_COLLAPSE_KEEP_SIGN_  = MINUS_; 
  private final static int ALL_VALS_FULL_COLLAPSE_KEEP_SIGN_ = UNDEFINED_;     
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  //
  // Pretty much a single big list of perturbation experiments
  //
  
  private HashMap<String, PertDataPoint> dataPoints_;
  private HashMap<String, PertSource> sourceDefs_;
  private HashMap<String, Experiment> experiments_;
  private HashMap<String, List<String>> targetGeneNotes_;  // Data from target gene notes field
  private HashMap<String, List<String>> dataPointNotes_;
  private HashMap<String, List<String>> userData_;
  private HashMap<String, String> investigators_;
  private HashMap<String, String> targets_;
  private HashMap<String, String> sourceNames_;
  private ConditionDictionary condDict_;
  
  private PertAnnotations pertAnnot_;
  
  private HashMap<String, RegionRestrict> regionRestrictions_;  // This used to live in timeSpan
  private NameMapper entryMap_;
  private NameMapper sourceMap_;
  private long serialNumber_;
  private long qpcrForDisplayVersionSN_;
  private long qpcrForDisplaySourceVersionSN_;
  private long qpcrForDisplayEntryVersionSN_;
  
  private PertDictionary pertDict_;
  private MeasureDictionary measureDict_;
  private UniqueLabeller labels_;
  private QpcrLegacyPublicExposed legacyQPCR_;
  private ArrayList<String> userFields_;
  
  private long sigPertCacheVersionSN_;
  private List<PertDataPoint> sigPertCache_;  
  
  private long invertSrcNameCacheVersionSN_;
  private HashMap invertSrcNameCache_;  
  
  private long invertTargNameCacheVersionSN_;
  private HashMap invertTargNameCache_;
  
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

  public PerturbationData(BTState appState) {
    appState_ = appState;
    labels_ = new UniqueLabeller();
    experiments_ = new HashMap<String, Experiment>();
    sourceDefs_ = new HashMap<String, PertSource>();
    dataPointNotes_ = new HashMap<String, List<String>>();
    userData_ = new HashMap<String, List<String>>();
    targetGeneNotes_ = new HashMap<String, List<String>>();
    dataPoints_ = new HashMap<String, PertDataPoint>();
    regionRestrictions_ = new HashMap<String, RegionRestrict>();
    entryMap_ = new NameMapper(appState_, "targets");
    sourceMap_ = new NameMapper(appState_, "sources");
    pertDict_ = new PertDictionary();
    measureDict_ = new MeasureDictionary();
    condDict_ = new ConditionDictionary();
    pertAnnot_ = new PertAnnotations();
    investigators_ = new HashMap<String, String>();
    targets_ = new HashMap<String, String>();
    sourceNames_ = new HashMap<String, String>();
    serialNumber_ = 0L; 
    qpcrForDisplayVersionSN_ = 0L;
    legacyQPCR_ = null;
    sigPertCacheVersionSN_ = 0L; 
    sigPertCache_ = null;
    invertSrcNameCacheVersionSN_ = 0L;
    invertSrcNameCache_ = null;   
    invertTargNameCacheVersionSN_ = 0L;
    invertTargNameCache_ = null;  
      
    userFields_ = new ArrayList<String>();
  }

  /***************************************************************************
  **
  ** Constructor
  */

  public PerturbationData(BTState appState, long serNum) {
    this(appState);
    serialNumber_ = serNum;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Get the choices for experiments
  */
  
  public int getExistingLegacyModes() { 
    int retval = NO_LEGACY_DATA;
    Iterator<PertDataPoint> dpit = dataPoints_.values().iterator();
    while (dpit.hasNext()) {
      PertDataPoint pdp = dpit.next();
      if (pdp.getLegacyPert() != null) {
        retval |= HAVE_LEGACY_PERT;
        break;
      }
    }
    Iterator<Experiment> oit = experiments_.values().iterator();
    while (oit.hasNext()) {
      Experiment exp = oit.next();
      if (exp.getLegacyMaxTime() != Experiment.NO_TIME) {
        retval |= HAVE_LEGACY_TIME_SPAN;
        break;
      }
    }
    Iterator<RegionRestrict> rit = regionRestrictions_.values().iterator();
    while (rit.hasNext()) {
      RegionRestrict rr = rit.next();
      if (rr.isLegacyNullStyle()) {
        retval |= HAVE_LEGACY_NULL_REGION;
        break;
      }
    }  
    return (retval);
  }  

  /***************************************************************************
  **
  ** Get the choices for experiments
  */
  
  public Vector<TrueObjChoiceContent> getExperimentOptions() { 
    TreeSet<TrueObjChoiceContent> sorted = new TreeSet<TrueObjChoiceContent>();
    Iterator<String> oit = experiments_.keySet().iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      sorted.add(getExperimentChoice(key));
    }
    return (new Vector<TrueObjChoiceContent>(sorted));
  }  
  
  /***************************************************************************
  **
  ** Get the choice for an experiment
  */
  
  public TrueObjChoiceContent getExperimentChoice(String key) {
    Experiment expr = experiments_.get(key);
    return (expr.getChoiceContent(this));
  }    

  /***************************************************************************
  **
  ** Get the choices for source names
  */
  
  public Vector<TrueObjChoiceContent> getSourceNameOptions() { 
    TreeSet<TrueObjChoiceContent> sorted = new TreeSet<TrueObjChoiceContent>();
    Iterator<String> oit = sourceNames_.keySet().iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      sorted.add(getSourceOrProxyNameChoiceContent(key));
    }
    return (new Vector<TrueObjChoiceContent>(sorted));
  }  
  
  /***************************************************************************
  **
  ** Get the choices for Targets
  */
  
  public Vector<TrueObjChoiceContent> getTargetOptions(boolean showAnnots) { 
    TreeSet<TrueObjChoiceContent> sorted = new TreeSet<TrueObjChoiceContent>();
    Iterator<String> oit = targets_.keySet().iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      sorted.add(getTargetChoiceContent(key, showAnnots));
    }
    return (new Vector<TrueObjChoiceContent>(sorted));
  }  
  
  /***************************************************************************
  **
  ** Get iterator over target keys
  */
  
  public Iterator<String> getTargetKeys() { 
    return (targets_.keySet().iterator());
  }  
  
  /***************************************************************************
  **
  ** get a DependencyAnalyzer
  */
  
  public DependencyAnalyzer getDependencyAnalyzer() {
    return (new DependencyAnalyzer(appState_, this));    
  } 
    
  /***************************************************************************
  **
  ** Transfer data from legacy QPCR into us
  */
  
  public void transferFromLegacy() {
    if (legacyQPCR_ != null) {
      pertDict_.createAllLegacyPerturbProps();
      legacyQPCR_.transferFromLegacy();
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Set legacy QPCR data
  */
  
  public void setLegacyQPCR(QpcrLegacyPublicExposed qpcr) {
    legacyQPCR_ = qpcr;
    return;
  } 
  
 /***************************************************************************
  **
  ** Answer if column definitions are "locked"
  */
  
  public boolean columnDefinitionsLocked() {
    Iterator<Experiment> oit = experiments_.values().iterator();
    while (oit.hasNext()) {
      Experiment exp = oit.next();
      if (exp.getLegacyMaxTime() != Experiment.NO_TIME) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Get the data point for the key:
  */
  
  public PertDataPoint getDataPoint(String key) {
    return (dataPoints_.get(key));
  } 

  /***************************************************************************
  **
  ** Answer of we have user fields:
  */
  
  public boolean haveUserFieldsDefined() {
    return (!userFields_.isEmpty());
  } 
  
  /***************************************************************************
  **
  ** Add a field for IO:
  */
  
  public void addUserFieldNameForIO(String name) {
    userFields_.add(name);
    return;
  } 
   
  /***************************************************************************
  **
  ** Get the names of the user-defined data fields:
  */
  
  public Iterator getUserFieldNames() {
    return (userFields_.iterator());
  } 
  
  /***************************************************************************
  **
  ** Get the set of names of the user-defined data fields:
  */
  
  public Set getUserFieldNameSet() {
    return (new HashSet(userFields_));
  } 

  /***************************************************************************
  **
  ** Get the number of user-defined data fields:
  */
  
  public int getUserFieldCount() {
    return (userFields_.size());
  }
  
  /***************************************************************************
  **
  ** Get the name of the specified user-defined data field:
  */
  
  public String getUserFieldName(int index) {
    return ((String)userFields_.get(index));
  }
    
  /***************************************************************************
  **
  ** Gotta be able to invert.  Returns an Integer, or null:
  */
  
  public Integer getUserFieldIndexFromNameAsInt(String name) {
    String normName = DataUtil.normKey(name);
    int numf = userFields_.size();
    for (int i = 0; i < numf; i++) {
      String ufn = (String)userFields_.get(i);
      if (DataUtil.normKey(ufn).equals(normName)) {
        return (new Integer(i));
      }
    }
    return (null);
  } 
  
   /***************************************************************************
  **
  ** Gotta be able to invert.  Returns a int as a string, or null:
  */
  
  public String getUserFieldIndexFromName(String name) {
    Integer retval = getUserFieldIndexFromNameAsInt(name);
    return ((retval == null) ? null : retval.toString());
  }
 
  /***************************************************************************
  **
  ** Get the value for the user field.  May be null.
  */
  
  public String getUserFieldValue(String pdpID, int index) {
    ArrayList list = (ArrayList)userData_.get(pdpID);
    if (list == null) {
      return (null);
    }
    if (index >= list.size()) {
      return (null);
    }
    return ((String)list.get(index));
  } 
 
  /***************************************************************************
  **
  ** Set the values for the user fields:
  */
  
  public PertDataChange setUserFieldValues(String pdpID, List<String> values) {
    if (userFields_.isEmpty()) {
      return (null);
    }
    if (pdpID == null) {
      throw new IllegalArgumentException();
    }
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.USER_FIELD_VALS);
    retval.userDataKey = pdpID;
    retval.userDataOrig = userData_.get(pdpID);  // may be null
    if ((values == null) || values.isEmpty()) {
      if (retval.userDataOrig == null) {  // no change...
        return (null);
      }
      retval.userDataOrig = new ArrayList<String>(retval.userDataOrig);
      userData_.remove(pdpID);
      retval.userDataNew = null;
    } else {
      retval.userDataOrig = (retval.userDataOrig == null) ? null : new ArrayList<String>(retval.userDataOrig);
      userData_.put(pdpID, new ArrayList<String>(values));
      retval.userDataNew = new ArrayList<String>(values);
    }   
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Undo a user field value change
  */
  
  private void userFieldValsUndo(PertDataChange undo) {
    if (undo.userDataOrig == null) { // Undo an add
      userData_.remove(undo.userDataKey);
    } else if (undo.userDataNew == null) {  // Undo a delete
      List<String> udo = new ArrayList<String>(undo.userDataOrig);
      userData_.put(undo.userDataKey, udo);
    } else {  // Change data
      List<String> udo = new ArrayList<String>(undo.userDataOrig);
      userData_.put(undo.userDataKey, udo);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a user field value change
  */
  
  private void userFieldValsRedo(PertDataChange redo) {
    if (redo.userDataOrig == null) { // Redo an add
      List<String> udn = new ArrayList<String>(redo.userDataNew);
      userData_.put(redo.userDataKey, udn);
    } else if (redo.userDataNew == null) {  // Redo a delete
      userData_.remove(redo.userDataKey);
    } else {  // Change data
      List<String> udn = new ArrayList<String>(redo.userDataNew);
      userData_.put(redo.userDataKey, udn);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 
  
  /***************************************************************************
  **
  ** Undo a user field name change
  */
  
  private void userFieldNameUndo(PertDataChange undo) {
    if (undo.userFieldNameOrig == null) { // Undo an add
      userFields_.remove(undo.userFieldIndex);
    } else if (undo.userFieldNameNew == null) {  // Undo a delete
      userFields_.add(undo.userFieldIndex, undo.userFieldNameOrig);
      Iterator<String> udkit = undo.userDataSubsetOrig.keySet().iterator();
      userData_.clear();
      while (udkit.hasNext()) {
        String key = udkit.next();
        List<String> ud = undo.userDataSubsetOrig.get(key);
        List<String> udc = new ArrayList<String>(ud);
        userData_.put(key, udc);
      }    
    } else {  // Change data
      userFields_.set(undo.userFieldIndex, undo.userFieldNameOrig);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a user field name change
  */
  
  private void userFieldNameRedo(PertDataChange redo) {
    if (redo.userFieldNameOrig == null) { // Redo an add
      userFields_.add(redo.userFieldNameNew);
    } else if (redo.userFieldNameNew == null) {  // Redo a delete
      userFields_.remove(redo.userFieldIndex);
      Iterator<String> udkit = redo.userDataSubsetNew.keySet().iterator();
      userData_.clear();
      while (udkit.hasNext()) {
        String key = udkit.next();
        List<String> ud = redo.userDataSubsetNew.get(key);
        List<String> udc = new ArrayList<String>(ud);
        userData_.put(key, udc);
      }    
    } else {  // Change data
      userFields_.set(redo.userFieldIndex, redo.userFieldNameNew);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }   
  
  /***************************************************************************
  **
  ** Set the name of a user field:
  */
  
  public PertDataChange setUserFieldName(String indexStr, String name) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.USER_FIELD_NAME);
    int index;
    try {
      index = Integer.parseInt(indexStr);
    } catch (NumberFormatException nfex) {
      throw new IllegalArgumentException();
    }
    int currentSize = userFields_.size();
    if (index > currentSize) {  // if adding one new field, will be equal
      throw new IllegalArgumentException();
    }
    retval.userFieldIndex = index;      
    retval.userFieldNameOrig = (index == currentSize) ? null : (String)userFields_.get(index);
    if (index == currentSize) {
      //
      // Note that the code knows how to handle a list that is too short, so we don't need to
      // do anything
      //
      userFields_.add(name);   
    } else {
      userFields_.set(index, name);
    }   
    retval.userFieldNameNew = name ;   
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Note this causes the entire user data set to be modified with the
  ** loss of the field.  IndexStr arg is a string representation of the index number.
  */ 
  
  public PertDataChange deleteUserFieldName(String indexStr) { 
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.USER_FIELD_NAME);

    // Stash original user data:
    retval.userDataSubsetOrig = new HashMap<String, List<String>>();
    Iterator<String> kit = userData_.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      List<String> userDataList = userData_.get(key);
      retval.userDataSubsetOrig.put(key, new ArrayList<String>(userDataList));
    }

    int index;
    try {
      index = Integer.parseInt(indexStr);
    } catch (NumberFormatException nfex) {
      throw new IllegalArgumentException();
    }
    
    int currentSize = userFields_.size();
    if (index >= currentSize) {
      throw new IllegalArgumentException();
    }
    retval.userFieldIndex = index;      
    retval.userFieldNameOrig = userFields_.get(index);
    userFields_.remove(index);   

    // Delete user data:
    kit = userData_.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      List<String> userDataList = userData_.get(key);
      // Handle "short" lists correctly:
      if (index < userDataList.size()) {
        userDataList.remove(index);
      }
    }    
    // Stash modified user data:
    retval.userDataSubsetNew = new HashMap<String, List<String>>();
    kit = userData_.keySet().iterator();
    while (kit.hasNext()) {
      String key = kit.next();
      List<String> userDataList = userData_.get(key);
      retval.userDataSubsetNew.put(key, new ArrayList<String>(userDataList));
    }
 
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  } 

  /***************************************************************************
  **
  ** Get the experimental conditions dictionary:
  */
  
  public ConditionDictionary getConditionDictionary() {
    return (condDict_);
  }   
  
  /***************************************************************************
  **
  ** Get the perturbation dictionary:
  */
  
  public PertDictionary getPertDictionary() {
    return (pertDict_);
  }  
 
  /***************************************************************************
  **
  ** Get the measurement dictionary:
  */
  
  public MeasureDictionary getMeasureDictionary() {
    return (measureDict_);
  }  
  
  /***************************************************************************
  **
  ** Get all the annotations:
  */
  
  public SortedMap getPertAnnotationsMap() {
    return (pertAnnot_.getFullMap());
  }
  
  /***************************************************************************
  **
  ** Get all the annotations:
  */
  
  public PertAnnotations getPertAnnotations() {
    return (pertAnnot_);
  }
 
  /***************************************************************************
  **
  ** Drop all cached display state:
  */
  
  public void dropCachedDisplayState() {
    if (legacyQPCR_ == null) {
      return;
    } 
    legacyQPCR_.dropCurrentStateForDisplay();
    qpcrForDisplayVersionSN_ = 0L;
    qpcrForDisplaySourceVersionSN_ = 0L;
    qpcrForDisplayEntryVersionSN_ = 0L;   
    return;
  }
      
  /***************************************************************************
  **
  ** Get qpcr for display:
  */
  
  private void stockQPCR() {
    if (legacyQPCR_ == null) {
      legacyQPCR_ = new QpcrLegacyPublicExposed(appState_);
    }    
    long srcSN = sourceMap_.getSerialNumber();
    long trgSN = entryMap_.getSerialNumber();
    if (!legacyQPCR_.readyForDisplay() || 
        (qpcrForDisplayVersionSN_ != serialNumber_) ||
        (qpcrForDisplaySourceVersionSN_ != srcSN) ||
        (qpcrForDisplayEntryVersionSN_ != trgSN)) {
      legacyQPCR_.createQPCRFromPerts(this);      
      qpcrForDisplayVersionSN_ = serialNumber_;
      qpcrForDisplaySourceVersionSN_ = srcSN;
      qpcrForDisplayEntryVersionSN_ = trgSN;
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get qpcr for display:
  */
  
  public String getHTML(String geneId, String sourceID, boolean noCss, boolean bigScreen) {
    stockQPCR();
    String ret = legacyQPCR_.getHTML(geneId, sourceID, noCss, bigScreen);
    return (ret);
  }
  
  /***************************************************************************
  **
  ** Publish to file:
  */
  
  public boolean publish(PrintWriter out) {
    stockQPCR();
    return (legacyQPCR_.publish(out));
  }
  
  /***************************************************************************
  **
  ** Publish CSV to file:
  */
  
  public boolean publishAsCSV(PrintWriter out) {
  
    /*
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
    
    */
    
  
    StringBuffer buf = new StringBuffer();
    TimeAxisDefinition tad = appState_.getDB().getTimeAxisDefinition();
    
    String units = tad.unitDisplayString();
       
    TreeMap<String, SortedSet<String>> expKeys = new TreeMap<String, SortedSet<String>>();
    
    Iterator<String> oit = experiments_.keySet().iterator();
    while (oit.hasNext()) {
      String expKey = oit.next();
      Experiment exp = experiments_.get(expKey);
      buf.setLength(0);
      buf.append(exp.getInvestigatorDisplayString(this).toUpperCase());
      buf.append("::---BT---::");
      buf.append(exp.getPertDisplayString(this, PertSources.NO_FOOTS).toUpperCase());
      buf.append("::---BT---::");
      buf.append(exp.getTimeDisplayString(false, true));
      String ord = buf.toString();
      SortedSet<String> hs = expKeys.get(ord);
      if (hs == null) {
        hs = new TreeSet<String>();
        expKeys.put(ord, hs);     
      }
      hs.add(expKey);
    }
 
    Iterator<SortedSet<String>> expit = expKeys.values().iterator();
    while (expit.hasNext()) {
      SortedSet<String> exKeySet = expit.next();
      Iterator<String> exksit = exKeySet.iterator();
      while (exksit.hasNext()) {
        String expKey = exksit.next();
        Experiment exp = experiments_.get(expKey);
        TreeSet dates = new TreeSet();
        TreeSet measureScales = new TreeSet();
        TreeSet measureProps = new TreeSet();
        TreeMap pointKeys = new TreeMap();
        TreeSet controls = new TreeSet();
       
        TreeSet used = new TreeSet();

        Iterator<String> dpit = dataPoints_.keySet().iterator();
        while (dpit.hasNext()) {
          String dpKey = dpit.next();
          PertDataPoint pdp = dataPoints_.get(dpKey);
          String dpExpKey = pdp.getExperimentKey();       
          if (dpExpKey.equals(expKey)) {          
            String targ = pdp.getTargetName(this).toUpperCase();
            String bid = pdp.getBatchKey();
            buf.setLength(0);
            buf.append(targ);
            buf.append("::--------BT---------::");
            buf.append(bid);
            String ord = buf.toString();
            TreeSet hs = (TreeSet)pointKeys.get(ord);
            if (hs == null) {
              hs = new TreeSet();
              pointKeys.put(ord, hs);     
            }
            hs.add(dpKey);
            String date = pdp.getDate();
            if (date == null) {
              date = "01/01/1970";
            }
            dates.add(date);
            String mTypeKey = pdp.getMeasurementTypeKey();
            measureProps.add(mTypeKey);
            MeasureProps mType = measureDict_.getMeasureProps(mTypeKey);
            measureScales.add(mType.getScaleKey());         
            String ctrlKey = pdp.getControl();
            if (ctrlKey != null) {
              controls.add(ctrlKey);
            }
            pdp.getAnnotationIDs(used, this);            
          }
        }
        
        if (pointKeys.isEmpty()) {
          continue;
        }
       
        String invests = exp.getInvestigatorDisplayString(this);
        /*
        List invList = exp.getInvestigators();
        int numInv = invList.size();
        if (numInv == 0) {
          out.println("\"Unknown Investigator\"");
        } else {
          for (int i = 0; i < numInv; i++) {
            String invest = (String)invList.get(i);
            out.print("\"");
            out.print(getInvestigator(invest));
            out.print("\"");
            if (i < (numInv - 1)) {
              out.print(",");
            }
          }
          out.println();
        }
         */

        /*
        out.print("\"TimeScale\",\"");      
        out.print(units);
        out.println("\"");

        Iterator msit = measureScales.iterator();
        while (msit.hasNext()) {
          String msKey = (String)msit.next();
          MeasureScale mScale = measureDict_.getMeasureScale(msKey);
          out.print("\"MeasureScale\",\"");      
          out.print(mScale.getName());
          out.print("\",\"");
          out.print(mScale.getUnchanged());
          out.print("\",\"");
          MeasureScale.Conversion conv = mScale.getConvToFold();
          out.print(MeasureScale.Conversion.mapTypeToTag(conv.type));
          out.print("\",\"");
          if (conv.factor != null) {
            out.print(conv.factor);
          }
          out.println("\"");
        }

        Iterator cit = controls.iterator();
        while (cit.hasNext()) {
          String cKey = (String)cit.next();
          ExperimentControl ctrl = condDict_.getExprControl(cKey);
          out.print("\"Control\",\"");      
          out.print(ctrl.getDescription());
          out.println("\"");
        }
        
        out.print("\"Condition\",\"");      
        out.print(exp.getCondsDisplayString(this));
        out.println("\"");
        */
        
        Iterator uit = used.iterator();
        while (uit.hasNext()) {
          String aKey = (String)uit.next(); 
          out.print("\"Annot\",\"");      
          out.print(pertAnnot_.getTag(aKey));
          out.print("\",\"");
          out.print(pertAnnot_.getMessage(aKey));
          out.println("\"");
        }
   
        /*
        Iterator mpit = measureProps.iterator();
        while (mpit.hasNext()) {
          String mpKey = (String)mpit.next();
          MeasureProps mType = measureDict_.getMeasureProps(mpKey);
          out.print("\"MeasureType\",\"");      
          out.print(mType.getName());
          out.print("\",\"");
          MeasureScale mScale = measureDict_.getMeasureScale(mType.getScaleKey());
          out.print(mScale.getName());
          out.print("\",\"");
          Double nt = mType.getNegThresh();
          if (nt != null) {
            out.print(nt);
          }
          out.print("\",\"");
          Double pt = mType.getPosThresh();
          if (pt != null) {
            out.print(pt);
          }
          out.println("\"");
        }
        
        TreeSet pertProps = new TreeSet();
        PertSources pss = exp.getSources();
        Iterator psit = pss.getSources();
        while (psit.hasNext()) {
          String srcID = (String)psit.next();
          PertSource ps = getSourceDef(srcID);
          pertProps.add(ps.getExpTypeKey());
        }      
        Iterator ppit = pertProps.iterator();
        while (ppit.hasNext()) {
          String ppKey = (String)ppit.next();
          PertProperties pType = pertDict_.getPerturbProps(ppKey);
          out.print("\"PerturbationType\",\"");      
          out.print(pType.getType());
          out.print("\",\"");
          String abr = pType.getAbbrev();
          if (abr != null) {
            out.print(abr);
          }
          out.print("\",\"");
          out.print(PertDictionary.mapPertLinkValToTag(pType.getLinkSignRelationship()));
          out.println("\"");
        }
        */
        /*
        Iterator dit = dates.iterator();
        while (dit.hasNext()) {
          String date = (String)dit.next();
          out.print("\"Date\",\"");      
          out.print(date);
          out.println("\"");
        }
       */

     //   out.println();
       out.println("\"BatchID\",\"PerturbationAgent\",\"MeasuredGene\",\"Time\",\"Measurement\",\"MeasureType\",\"ExpControl\",\"ForceSignificance\",\"Date\",\"Comments\",\"Annot\",\"RegRestrict\"");
    //   out.println("\"BatchID\",\"PerturbationAgent\",\"MeasuredGene\",\"Time\",\"Measurement\",\"ForceSignificance\",\"Comments\",\"Annot\"");

        Iterator pkit = pointKeys.values().iterator();
        while (pkit.hasNext()) {
          TreeSet dpKeySet = (TreeSet)pkit.next();
          Iterator dpksit = dpKeySet.iterator();
          while (dpksit.hasNext()) {
            String dpKey = (String)dpksit.next();
            PertDataPoint pdp = dataPoints_.get(dpKey);
            RegionRestrict rr = getRegionRestrictionForDataPoint(dpKey);
            pdp.publishAsCSV(out, this, exp, condDict_, measureDict_, pertAnnot_, rr, invests);         
          }       
        } 
        out.println();
      }
    }
          
    return (true);
  }
  
  /***************************************************************************
  **
  ** Get a label key:
  */
  
  public String getNextDataKey() {
    return (labels_.getNextLabel());
  }
 
  /***************************************************************************
  **
  ** Get a sorted, comma separated string of footnote numbers for the note IDs:
  */
  
  public String getFootnoteListAsString(List noteIDs) {
    return (pertAnnot_.getFootnoteListAsString(noteIDs));   
  }
  
  /***************************************************************************
  **
  ** Get a sorted, comma separated string of "footnote numbers = text" for the note IDs:
  */
  
  public String getFootnoteListAsNVString(List noteIDs) {
    return (pertAnnot_.getFootnoteListAsNVString(noteIDs));   
  }
   
  /***************************************************************************
  **
  ** Get a sorted list of footnote numbers for the note IDs:
  */
  
  public List getFootnoteList(List noteIDs) {
    return (pertAnnot_.getFootnoteList(noteIDs));   
  } 
  
  /***************************************************************************
  **
  ** Add footnote
  */
  
  public String addLegacyMessage(String key, String message) {
    return (pertAnnot_.addLegacyMessage(key, message));   
  }
  
  /***************************************************************************
  **
  ** Add footnote
  */
  
  public String addMessage(String message) {
    return (pertAnnot_.addMessage(message));   
  }
 
  /***************************************************************************
  **
  ** Get the count of investigators:
  */
  
  public int getInvestigatorCount() {
    return (investigators_.size());
  }

  /***************************************************************************
  **
  ** Get the keys for the investigators:
  */
  
  public Iterator<String> getInvestigatorKeys() {
    return (investigators_.keySet().iterator());
  }

  /***************************************************************************
  **
  ** Get the investigator for the given key:
  */
  
  public String getInvestigator(String key) {
    return (investigators_.get(key));
  }
  
  /***************************************************************************
  **
  ** Get all the investigators
  */
  
  public Set<String> getInvestigatorSet() {
    return (new HashSet<String>(investigators_.values()));
  }  
    
  /***************************************************************************
  **
  ** Get the perturbation target for the given key:
  */
  
  public String getTarget(String key) {
    return (targets_.get(key));
  }

 /***************************************************************************
  **
  ** Get all the targets
  */
  
  public Set getTargetSet() {
    return (new HashSet(targets_.values()));
  }

  /***************************************************************************
  **
  ** Get all the source names
  */
  
  public Set<String> getSourceNameSet() {
    return (new HashSet(sourceNames_.values()));
  }  
  
  /***************************************************************************
  **
  ** Get an iterator over source name keys:
  */
  
  public Iterator<String> getSourceNameKeys() {
    return (sourceNames_.keySet().iterator());
  }  
  
  /***************************************************************************
  **
  ** Get the perturbation source name for the given key:
  */
  
  public String getSourceName(String key) {
    return (sourceNames_.get(key));
  }  
  
  /***************************************************************************
  **
  ** Get the perturbation source or target for the given key:
  */
  
  public String getSourceOrTarget(String key) {
    String retval = (String)targets_.get(key);
    if (retval == null) {
      retval = sourceNames_.get(key);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** When building up networks of perturbation data, we need to be able to answer if
  ** a source name key and a target name key are the same
  */
  
  public boolean sourceTargetEquivalent(String sKey, String tKey) {
    String tName = (String)targets_.get(tKey);
    String sName = sourceNames_.get(sKey);
    return (DataUtil.keysEqual(tName, sName));
  }

  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  public String getInvestKeyFromName(String name) {
    String normName = DataUtil.normKey(name);
    Iterator ikit = investigators_.keySet().iterator();
    while (ikit.hasNext()) {
      String iKey = (String)ikit.next();
      String inv = (String)investigators_.get(iKey);
      if (DataUtil.normKey(inv).equals(normName)) {
        return (iKey);
      }
    }
    return (null);
  } 
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  public String getSourceKeyFromName(String name) {
    String normName = DataUtil.normKey(name);
    Map isnc = buildInvertSrcNameCache(); 
    return ((String)isnc.get(normName));
  }  
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  private Map buildInvertSrcNameCache() {
    if ((invertSrcNameCache_ == null) || (invertSrcNameCacheVersionSN_ != serialNumber_)) {
      invertSrcNameCache_ = new HashMap();
      Iterator<String> ikit = sourceNames_.keySet().iterator();
      while (ikit.hasNext()) {
        String srcKey = ikit.next();
        String src = sourceNames_.get(srcKey);
        invertSrcNameCache_.put(DataUtil.normKey(src), srcKey);
      }
      invertSrcNameCacheVersionSN_ = serialNumber_;
    }
    return (invertSrcNameCache_);
  }  

  /***************************************************************************
  **
  ** Get target key for the name
  */
  
  public String getTargKeyFromName(String name) {
    String normName = DataUtil.normKey(name);
    Map itnc = buildInvertTrgNameCache();
    return ((String)itnc.get(normName));
  }  
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  private Map buildInvertTrgNameCache() {
    if ((invertTargNameCache_ == null) || (invertTargNameCacheVersionSN_ != serialNumber_)) {
      invertTargNameCache_ = new HashMap();
      Iterator ikit = targets_.keySet().iterator();
      while (ikit.hasNext()) {
        String trgKey = (String)ikit.next();
        String trg = (String)targets_.get(trgKey);
        invertTargNameCache_.put(DataUtil.normKey(trg), trgKey);
      }
      invertTargNameCacheVersionSN_ = serialNumber_;
    }
    return (invertTargNameCache_);
  }  
  
  /***************************************************************************
  **
  ** Answer if the name shows up in the source table
  */
  
  public boolean isSourceName(String geneName) {
    String normName = DataUtil.normKey(geneName);
    Iterator<String> snit = sourceNames_.values().iterator();
    while (snit.hasNext()) {
      String checkName = snit.next();
      if (normName.equals(DataUtil.normKey(checkName))) {
        return (true);
      }
    }
    return (false);
  }  

  /***************************************************************************
  **
  ** Get the perturbation source name for the given key:
  */
  
  public boolean isTargetName(String geneName) {
    String normName = DataUtil.normKey(geneName);
    Iterator ikit = targets_.keySet().iterator();
    while (ikit.hasNext()) {
      String trgKey = (String)ikit.next();
      String trg = (String)targets_.get(trgKey);
      if (normName.equals(DataUtil.normKey(trg))) {
        return (true);
      }
    }
    return (false);
  } 
  
  /***************************************************************************
  **
  ** Set user data for a data point:
  */
  
  public void setUserDataForIO(String pdID, Map indexToVals) {
    ArrayList myData = new ArrayList();
    int numVals = userFields_.size();
    for (int i = 0; i < numVals; i++) {
      String strIndx = Integer.toString(i);
      String val = (String)indexToVals.get(strIndx);
      if (val == null) {
        val = "";
      }
      myData.add(val);
    }
    userData_.put(pdID, myData);
    return;
  }

  /***************************************************************************
  **
  ** Set region restrictions for a data point for IO only:
  */
  
  public void setRegionRestrictionForDataPointForIO(String pdID, RegionRestrict regReg) {
    regionRestrictions_.put(pdID, regReg);
    return;
  }
   
  /***************************************************************************
  **
  ** Set region restrictions for a data point:
  */
  
  public PertDataChange setRegionRestrictionForDataPoint(String pdpID, RegionRestrict regReg) { 
    if (pdpID == null) {
      throw new IllegalArgumentException();
    }   
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.REG_RESTRICT);
    retval.dataRegResKey = pdpID;
    retval.dataRegResOrig = (RegionRestrict)regionRestrictions_.get(pdpID);  // may be null
    if (regReg == null) {
      if (retval.dataRegResOrig == null) {  // no change...
        return (null);
      }
      retval.dataRegResOrig = (RegionRestrict)retval.dataRegResOrig.clone();
      regionRestrictions_.remove(pdpID);
      retval.dataRegResNew = null;
    } else {
      retval.dataRegResOrig = (retval.dataRegResOrig == null) ? null : (RegionRestrict)retval.dataRegResOrig.clone();
      regionRestrictions_.put(pdpID, regReg.clone());
      retval.dataRegResNew = (RegionRestrict)regReg.clone();
    }   
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo region restriction operation:
  */
  
  public void regRestrictUndo(PertDataChange undo) { 
    if (undo.dataRegResOrig == null) { // Undo an add
      regionRestrictions_.remove(undo.dataRegResKey);
    } else if (undo.dataRegResNew == null) {  // Undo a delete
      RegionRestrict rro = (RegionRestrict)undo.dataRegResOrig.clone();
      regionRestrictions_.put(undo.dataRegResKey, rro);
    } else {  // Change data
      RegionRestrict rro = (RegionRestrict)undo.dataRegResOrig.clone();
      regionRestrictions_.put(undo.dataRegResKey, rro);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  /***************************************************************************
  **
  ** Redo region restriction operation:
  */
  
  public void regRestrictRedo(PertDataChange redo) { 
    if (redo.dataRegResOrig == null) { // Redo an add
      RegionRestrict rrn = (RegionRestrict)redo.dataRegResNew.clone();
      regionRestrictions_.put(redo.dataRegResKey, rrn);
    } else if (redo.dataRegResNew == null) {  // Redo a delete
      regionRestrictions_.remove(redo.dataRegResKey);
    } else {  // Change data
      RegionRestrict rrn = (RegionRestrict)redo.dataRegResNew.clone();
      regionRestrictions_.put(redo.dataRegResKey, rrn);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 

  /***************************************************************************
  **
  ** get region restrictions for a data point (may be null)
  */
  
  public RegionRestrict getRegionRestrictionForDataPoint(String pdID) {
    return ((RegionRestrict)regionRestrictions_.get(pdID));
  }
  
  /***************************************************************************
  **
  ** Change region name in region restrictions 
  */
  
  public PertDataChange[] changeRegionNameForRegionRestrictions(String oldName, String newName) {
    ArrayList allRets = new ArrayList();
    Iterator rrit = regionRestrictions_.keySet().iterator();
    while (rrit.hasNext()) {
      String key = (String)rrit.next();
      RegionRestrict regReg = (RegionRestrict)regionRestrictions_.get(key);
      RegionRestrict changed = regReg.changeRegionName(oldName, newName);
      if (changed != null) {
        PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.REG_RESTRICT);
        retval.dataRegResKey = key;
        retval.dataRegResOrig = (RegionRestrict)regReg.clone();
        regionRestrictions_.put(key, changed);
        retval.dataRegResNew = (RegionRestrict)changed.clone();
        retval.serialNumberNew = ++serialNumber_;
        allRets.add(retval);
      }
    }
    PertDataChange[] changes = (PertDataChange[])allRets.toArray(new PertDataChange[allRets.size()]);
    return (changes);
  }  
  
  /***************************************************************************
  **
  ** Drop region name in region restrictions 
  */
  
  public PertDataChange[] dropRegionNameForRegionRestrictions(String dropName) {
    ArrayList allRets = new ArrayList();
    HashSet testSet = new HashSet();
    testSet.add(dropName);
    Iterator rrit = new HashSet(regionRestrictions_.keySet()).iterator();
    while (rrit.hasNext()) {
      String key = (String)rrit.next();
      RegionRestrict regReg = (RegionRestrict)regionRestrictions_.get(key);
      if (!regReg.containsRegionName(testSet)) {
        continue;
      } 
      RegionRestrict dropped = regReg.dropRegionName(dropName);
      PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.REG_RESTRICT);
      retval.dataRegResKey = key;
      retval.dataRegResOrig = (RegionRestrict)regReg.clone();
      if (dropped == null) {
        regionRestrictions_.remove(key);
        retval.dataRegResNew = null;
      } else {
        regionRestrictions_.put(key, dropped);
        retval.dataRegResNew = (RegionRestrict)dropped.clone();
      }
      retval.serialNumberNew = ++serialNumber_;
      allRets.add(retval);
    }
    PertDataChange[] changes = (PertDataChange[])allRets.toArray(new PertDataChange[allRets.size()]);
    return (changes);
  }  
  
  /***************************************************************************
  **
  ** Find region name in region restrictions 
  */
  
  public boolean haveRegionNameInRegionRestrictions(Set dropNames) {
    Iterator rrit = regionRestrictions_.keySet().iterator();
    while (rrit.hasNext()) {
      String key = (String)rrit.next();
      RegionRestrict regReg = (RegionRestrict)regionRestrictions_.get(key);
      if (regReg.containsRegionName(dropNames)) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Set footnotes for a data point for IO only
  */
  
  public void setFootnotesForDataPointForIO(String pdID, List<String> noteMsgIDs) {
    dataPointNotes_.put(pdID, new ArrayList<String>(noteMsgIDs));
    return;
  }
  
  /***************************************************************************
  **
  ** Set footnotes for a data point
  */
  
  public PertDataChange setFootnotesForDataPoint(String pdpID, List noteMsgIDs) {
    if (pdpID == null) {
      throw new IllegalArgumentException();
    }   
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.DATA_ANNOTS);
    retval.dataAnnotsKey = pdpID;
    retval.dataAnnotsOrig = (ArrayList)dataPointNotes_.get(pdpID);  // may be null
    if ((noteMsgIDs == null) || noteMsgIDs.isEmpty()) {
      if (retval.dataAnnotsOrig == null) {  // no change...
        return (null);
      }
      retval.dataAnnotsOrig = (ArrayList)retval.dataAnnotsOrig.clone();
      dataPointNotes_.remove(pdpID);
      retval.dataAnnotsNew = null;
    } else {
      retval.dataAnnotsOrig = (retval.dataAnnotsOrig == null) ? null : (ArrayList)retval.dataAnnotsOrig.clone();
      dataPointNotes_.put(pdpID, new ArrayList(noteMsgIDs));
      retval.dataAnnotsNew = new ArrayList(noteMsgIDs);
    }   
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }

  /***************************************************************************
  **
  ** Undo data annotation operation:
  */
  
  public void dataAnnotUndo(PertDataChange undo) { 
    if (undo.dataAnnotsOrig == null) { // Undo an add
      dataPointNotes_.remove(undo.dataAnnotsKey);
    } else if (undo.dataAnnotsNew == null) {  // Undo a delete
      ArrayList dao = (ArrayList)undo.dataAnnotsOrig.clone();
      dataPointNotes_.put(undo.dataAnnotsKey, dao);
    } else {  // Change data
      ArrayList dao = (ArrayList)undo.dataAnnotsOrig.clone();
      dataPointNotes_.put(undo.dataAnnotsKey, dao);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo data annotation operation:
  */
  
  public void dataAnnotRedo(PertDataChange redo) { 
    if (redo.dataAnnotsOrig == null) { // Redo an add
      ArrayList dan = (ArrayList)redo.dataAnnotsNew.clone();
      dataPointNotes_.put(redo.dataAnnotsKey, dan);
    } else if (redo.dataAnnotsNew == null) {  // Redo a delete
      dataPointNotes_.remove(redo.dataAnnotsKey);
    } else {  // Change data
      ArrayList dan = (ArrayList)redo.dataAnnotsNew.clone();
      dataPointNotes_.put(redo.dataAnnotsKey, dan);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 

  /***************************************************************************
  **
  ** Set footnotes for a target
  */
  
  public void setFootnotesForTargetIO(String targKey, List<String> noteMsgIDs) {
    targetGeneNotes_.put(targKey, new ArrayList<String>(noteMsgIDs));
    return;
  }
  
  /***************************************************************************
  **
  ** Set footnotes for a target (now only used internally)
  */
  
  private PertDataChange setFootnotesForTarget(String targKey, List noteMsgIDs) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.TARGET_ANNOTS);
    retval.targetAnnotKey = targKey;
    retval.targetAnnotOrig = (ArrayList)targetGeneNotes_.get(targKey);  // may be null
    if ((noteMsgIDs == null) || noteMsgIDs.isEmpty()) {
      if (retval.targetAnnotOrig == null) {  // no change...
        return (null);
      }
      retval.targetAnnotOrig = (ArrayList)retval.targetAnnotOrig.clone();
      targetGeneNotes_.remove(targKey);
      retval.targetAnnotNew = null;
    } else {
      retval.targetAnnotOrig = (retval.targetAnnotOrig == null) ? null : (ArrayList)retval.targetAnnotOrig.clone();
      targetGeneNotes_.put(targKey, new ArrayList(noteMsgIDs));
      retval.targetAnnotNew = new ArrayList(noteMsgIDs);
    }   
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo target annotation operation:
  */
  
  public void targetAnnotUndo(PertDataChange undo) { 
    if (undo.targetAnnotOrig == null) { // Undo an add
      targetGeneNotes_.remove(undo.targetAnnotKey);
    } else if (undo.targetAnnotNew == null) {  // Undo a delete
      ArrayList tao = (ArrayList)undo.targetAnnotOrig.clone();
      targetGeneNotes_.put(undo.targetAnnotKey, tao);
    } else {  // Change data
      ArrayList tao = (ArrayList)undo.targetAnnotOrig.clone();
      targetGeneNotes_.put(undo.targetAnnotKey, tao);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo target annotation operation:
  */
  
  public void targetAnnotRedo(PertDataChange redo) { 
    if (redo.targetAnnotOrig == null) { // Redo an add
      ArrayList tan = (ArrayList)redo.targetAnnotNew.clone();
      targetGeneNotes_.put(redo.targetAnnotKey, tan);
    } else if (redo.targetAnnotNew == null) {  // Redo a delete
      targetGeneNotes_.remove(redo.targetAnnotKey);
    } else {  // Change data
      ArrayList tan = (ArrayList)redo.targetAnnotNew.clone();
      targetGeneNotes_.put(redo.targetAnnotKey, tan);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }   
  
  /***************************************************************************
  **
  ** Get footnotes for a target
  */
  
  public List getFootnotesForTarget(String targKey) {
    return ((List)targetGeneNotes_.get(targKey));
  }
  
  /***************************************************************************
  **
  ** Get target display string, with annotations:
  */
  
  public String getAnnotatedTargetDisplay(String targKey) {
    List foots = (List)targetGeneNotes_.get(targKey);
    String targ = (String)targets_.get(targKey);
    if ((foots == null) || foots.isEmpty()) {
      return (targ);
    }
    StringBuffer buf = new StringBuffer();  
    buf.append(targ);
    buf.append(" [");
    buf.append(getFootnoteListAsString(foots));
    buf.append("]");
    return (buf.toString());
  }
  
  /***************************************************************************
  **
  ** Get the count of experiments
  */
  
  public int getExperimentCount() {
    return (experiments_.size());
  }
  
  /***************************************************************************
  **
  ** Return, or create and return, a PertSourcesInfo that matches:
  */
  
  public KeyAndDataChange provideExperiment(PertSources srcs, int time, int legacyMax, List investList, String condKey) {
    Experiment prs = new Experiment(appState_, "", srcs, time, investList, condKey);
    if (legacyMax != Experiment.NO_TIME) {
      prs.setLegacyMaxTime(legacyMax);
    }    
    Iterator prsit = experiments_.values().iterator();
    while (prsit.hasNext()) {
      Experiment chk = (Experiment)prsit.next();
      if (chk.equalsMinusID(prs)) {
        return (new KeyAndDataChange(chk.getID(), null));
      }
    }
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXPERIMENT);
    retval.expOrig = null;    
    String nextID = labels_.getNextLabel();
    prs.setID(nextID);
    retval.expNew = (Experiment)prs.clone();    
    experiments_.put(nextID, prs);
    retval.serialNumberNew = ++serialNumber_;
    return (new KeyAndDataChange(nextID, retval));
  }
   
  /***************************************************************************
  **
  ** Count of pert sources
  */
  
  public int getPertSourceCount() {
    return (sourceNames_.size());
  }
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  public String getPertSourceFromName(String srcName) {
    String normName = DataUtil.normKey(srcName);
    Iterator<String> snkit = sourceNames_.keySet().iterator();
    while (snkit.hasNext()) {
      String chkID = snkit.next();
      String chkName = sourceNames_.get(chkID);
      if (normName.equals(DataUtil.normKey(chkName))) {
        return (chkID);
      }
    }
    return (null);
  } 

  /***************************************************************************
  **
  ** Return, or create and return, a Pert Source Name that matches:
  */
  
  public KeyAndDataChange providePertSrcName(String srcName) {   
    String foundID = getPertSourceFromName(srcName);
    if (foundID != null) {
      return (new KeyAndDataChange(foundID, null));
    }

    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.SOURCE_NAME);
    retval.srcNameOrig = null;
    retval.srcNameKey = labels_.getNextLabel();
    retval.srcNameNew = srcName;
    sourceNames_.put(retval.srcNameKey, srcName);
    retval.serialNumberNew = ++serialNumber_;
    return (new KeyAndDataChange(retval.srcNameKey, retval));
  }
 
  /***************************************************************************
  **
  ** Undo source name operation:
  */
  
  private void sourceNameUndo(PertDataChange undo) {
    if (undo.srcNameOrig == null) { // Undo an add
      sourceNames_.remove(undo.srcNameKey);
      labels_.removeLabel(undo.srcNameKey);
    } else if (undo.srcNameNew == null) {  // Undo a delete
      sourceNames_.put(undo.srcNameKey, undo.srcNameOrig);
      labels_.addExistingLabel(undo.srcNameKey);
    } else {  // Change data
      sourceNames_.put(undo.srcNameKey, undo.srcNameOrig);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo source name operation:
  */
  
  private void sourceNameRedo(PertDataChange redo) { 
    if (redo.srcNameOrig == null) { // Redo an add
      sourceNames_.put(redo.srcNameKey, redo.srcNameNew);
      labels_.addExistingLabel(redo.srcNameKey);
    } else if (redo.srcNameNew == null) {  // Redo a delete
      sourceNames_.remove(redo.srcNameKey);
      labels_.removeLabel(redo.srcNameKey);
    } else {  // Change data
      sourceNames_.put(redo.srcNameKey, redo.srcNameNew);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 
  
  /***************************************************************************
  **
  ** Undo target name operation:
  */
  
  private void targetNameUndo(PertDataChange undo) {
    if (undo.targetOrig == null) { // Undo an add
      targets_.remove(undo.targetKey);
      labels_.removeLabel(undo.targetKey);
    } else if (undo.targetNew == null) {  // Undo a delete
      targets_.put(undo.targetKey, undo.targetOrig);
      labels_.addExistingLabel(undo.targetKey);
    } else {  // Change data
      targets_.put(undo.targetKey, undo.targetOrig);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo target name operation:
  */
  
  private void targetNameRedo(PertDataChange redo) { 
    if (redo.targetOrig == null) { // Redo an add
      targets_.put(redo.targetKey, redo.targetNew);
      labels_.addExistingLabel(redo.targetKey);
    } else if (redo.targetNew == null) {  // Redo a delete
      targets_.remove(redo.targetKey);
      labels_.removeLabel(redo.targetKey);
    } else {  // Change data
      targets_.put(redo.targetKey, redo.targetNew);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }
    
  /***************************************************************************
  **
  ** Return, or create and return, a PertSource that matches:
  */
  
  public KeyAndDataChange providePertSrc(String srcNameKey, String typeKey, 
                                         String proxyForKey, String proxySign, 
                                         List annotations, boolean justSrcAndType) {
    PertSource ps = new PertSource("", srcNameKey, typeKey, annotations);
    if (!proxySign.equals(PertSource.NO_PROXY)) {
      ps.setProxiedSpeciesKey(proxyForKey);
      ps.setProxySign(proxySign);
    }
    Iterator sdit = sourceDefs_.values().iterator();
    while (sdit.hasNext()) {
      PertSource chk = (PertSource)sdit.next();
      if (justSrcAndType) {
        if (chk.compareSrcAndType(ps) == 0) {
          return (new KeyAndDataChange(chk.getID(), null));
        }
      } else {
        if (chk.equalsMinusID(ps)) {
          return (new KeyAndDataChange(chk.getID(), null));
        }
      }
    }
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.SOURCE_DEF);
    retval.srcDefOrig = null;  
    String nextID = labels_.getNextLabel();
    ps.setID(nextID);
    sourceDefs_.put(nextID, ps);
    retval.srcDefNew = (PertSource)ps.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (new KeyAndDataChange(nextID, retval));
  }
  
  /***************************************************************************
  **
  ** Return, or create and return, a measure scale that matches;
  */
  
  public KeyAndDataChange provideMeasureScale(String scaleDesc, MeasureScale.Conversion convToFold, 
                                              BoundedDoubMinMax illegal, Double unchanged) {
    if (scaleDesc == null) {
      return (null);
    }
    String exist = measureDict_.getMeasureScaleFromName(scaleDesc);
    if (exist != null) {
      MeasureScale scale = measureDict_.getMeasureScale(exist);
      if (convToFold == null) {
        if (scale.getConvToFold() != null) {
          return (null);
        }
      } else if (!convToFold.equals(scale.getConvToFold())) {
        return (null);
      }
      if (illegal == null) {
        if (scale.getIllegalRange() != null) {
          return (null);
        }
      } else if (!illegal.equals(scale.getIllegalRange())) {
        return (null);
      }
      if (unchanged == null) {
        if (scale.getUnchanged() != null) {
          return (null);
        }
      } else if (!unchanged.equals(scale.getUnchanged())) {
        return (null);
      }
      
      return (new KeyAndDataChange(exist, null));
    }   
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.MEAS_SCALE);
    retval.mScaleOrig = null;
    String nextID = measureDict_.getNextDataKey();  
    MeasureScale scale = new MeasureScale(nextID, scaleDesc, convToFold, illegal, unchanged);
    measureDict_.setMeasureScale(scale);
    retval.mScaleNew = (MeasureScale)scale.clone();
    return (new KeyAndDataChange(nextID, retval));
  }
  
  /***************************************************************************
  **
  ** Return, or create and return, a measure properties that matches;
  */
  
  public KeyAndDataChange provideMeasureProps(String measName, String scaleKey, Double negThresh, Double posThresh) {
    if (measName == null) {
      return (null);
    }
    String exist = measureDict_.getMeasurePropsFromName(measName);
    if (exist != null) {
      MeasureProps mProps = measureDict_.getMeasureProps(exist);
      if ((!scaleKey.equals(mProps.getScaleKey())) ||
          (!negThresh.equals(mProps.getNegThresh())) || 
          (!posThresh.equals(mProps.getPosThresh())))  {
        return (null);
      }
      return (new KeyAndDataChange(exist, null));
    }   
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.MEASURE_PROP);
    retval.mpOrig = null;
    String nextID = measureDict_.getNextDataKey();  
    MeasureProps mProps = new MeasureProps(nextID, measName, scaleKey, negThresh, posThresh);
    measureDict_.setMeasureProp(mProps);
    retval.mpNew = (MeasureProps)mProps.clone();
    return (new KeyAndDataChange(nextID, retval));
  }
  
  /***************************************************************************
  **
  ** Return, or create and return, a pert properties that matches;
  */
  
  public KeyAndDataChange providePertProps(String propName, String abbrev, PertDictionary.PertLinkRelation linkRelation) {
    if (propName == null) {
      return (null);
    }
    String exist = pertDict_.getPerturbPropsFromName(propName);
    if (exist != null) {
      PertProperties pProps = pertDict_.getPerturbProps(exist);
      PertProperties chkProps = new PertProperties("", propName, abbrev, linkRelation);
      if (!chkProps.semiEquals(pProps)) {
        return (null);
      }
      return (new KeyAndDataChange(exist, null));
    }   
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.PERT_PROP);
    retval.ppOrig = null;
    String nextID = pertDict_.getNextDataKey(); 
    PertProperties pProps = new PertProperties(nextID, propName, abbrev, linkRelation);
    pertDict_.setPerturbProp(pProps);
    retval.ppNew = (PertProperties)pProps.clone();
    return (new KeyAndDataChange(nextID, retval));
  }
  
  /***************************************************************************
  **
  ** Undo pert prop operation:
  */
  
  private void pertPropUndo(PertDataChange undo) { 
    if (undo.ppOrig == null) { // Undo an add
      String rescueID = undo.ppNew.getID();
      pertDict_.dropPerturbProp(rescueID);
    } else if (undo.ppNew == null) {  // Undo a delete
      PertProperties ppr = (PertProperties)undo.ppOrig.clone();
      pertDict_.addPerturbPropForIO(ppr);  // Handles ID accounting
    } else {  // Change data
      PertProperties ppr = (PertProperties)undo.ppOrig.clone();
      pertDict_.setPerturbProp(ppr);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo pert prop operation:
  */
  
  private void pertPropRedo(PertDataChange redo) { 
    if (redo.ppOrig == null) { // Redo an add
      PertProperties ppr = (PertProperties)redo.ppNew.clone();
      pertDict_.addPerturbPropForIO(ppr);  // Handles ID accounting
    } else if (redo.ppNew == null) {  // Redo a delete
      String rescueID = redo.ppOrig.getID();
      pertDict_.dropPerturbProp(rescueID);
    } else {  // Change data
      PertProperties ppr = (PertProperties)redo.ppNew.clone();
      pertDict_.setPerturbProp(ppr); 
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 

  /***************************************************************************
  **
  ** Return, or create and return, a user field that matches:
  */
  
  public KeyAndDataChange provideUserField(String fieldName) {
    if (fieldName == null) {
      return (null);
    }
    
    String exist = getUserFieldIndexFromName(fieldName);
    if (exist != null) {
      return (new KeyAndDataChange(exist, null));
    }    
    String nextID = Integer.toString(getUserFieldCount());
    PertDataChange pdc = setUserFieldName(nextID, fieldName);
    return (new KeyAndDataChange(nextID, pdc));
  }
  
  /***************************************************************************
  **
  ** Return, or create and return, an experimental control that matches:
  */
  
  public KeyAndDataChange provideExpControl(String ctrlDesc) {
    if (ctrlDesc == null) {
      return (null);
    }
    String exist = condDict_.getControlKeyFromName(ctrlDesc);
    if (exist != null) {
      return (new KeyAndDataChange(exist, null));
    }   
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXP_CONTROL);
    retval.ectrlOrig = null;
    String nextID = condDict_.getNextDataKey();  
    ExperimentControl ctrl = new ExperimentControl(nextID, ctrlDesc);
    condDict_.setExprControl(ctrl);
    retval.ectrlNew = (ExperimentControl)ctrl.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (new KeyAndDataChange(nextID, retval));
  } 
  
  /***************************************************************************
  **
  ** Return, or create and return, an experimental condition that matches:
  */
  
  public KeyAndDataChange provideExpCondition(String condDesc) {
    if (condDesc == null) {
      return (null);
    }
    String exist = condDict_.getConditionKeyFromName(condDesc);
    if (exist != null) {
      return (new KeyAndDataChange(exist, null));
    }   
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXP_COND);
    retval.ecOrig = null;
    String nextID = condDict_.getNextDataKey();  
    ExperimentConditions eCond = new ExperimentConditions(nextID, condDesc);
    condDict_.setExprCondition(eCond);
    retval.ecNew = (ExperimentConditions)eCond.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (new KeyAndDataChange(nextID, retval));
  } 

  /***************************************************************************
  **
  ** Return, or create and return, an investigator that matches:
  */
  
  public KeyAndDataChange provideInvestigator(String investName) {
    String exist = getInvestKeyFromName(investName);
    if (exist != null) {
      return (new KeyAndDataChange(exist, null));
    }   
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.INVEST);
    retval.investOrig = null;
    String nextID = labels_.getNextLabel();
    retval.investKey = nextID;
    retval.investNew = investName;
    investigators_.put(nextID, investName);
    retval.serialNumberNew = ++serialNumber_;
    return (new KeyAndDataChange(nextID, retval));
  }
  
  /***************************************************************************
  **
  ** Undo investigator operation:
  */
  
  private void investUndo(PertDataChange undo) { 
    if (undo.investOrig == null) { // Undo an add
      investigators_.remove(undo.investKey);
      labels_.removeLabel(undo.investKey);
    } else if (undo.investNew == null) {  // Undo a delete
      investigators_.put(undo.investKey, undo.investOrig);
      labels_.addExistingLabel(undo.investKey);
    } else {  // Change data
      investigators_.put(undo.investKey, undo.investOrig);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo investigator operation:
  */
  
  private void investRedo(PertDataChange redo) { 
    if (redo.investOrig == null) { // Redo an add
      investigators_.put(redo.investKey, redo.investNew);
      labels_.addExistingLabel(redo.investKey);
    } else if (redo.investNew == null) {  // Redo a delete
      investigators_.remove(redo.investKey);
      labels_.removeLabel(redo.investKey);
    } else {  // Change data
      investigators_.put(redo.investKey, redo.investNew);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 
  
  /***************************************************************************
  **
  ** Set the investigator:
  */
  
  public PertDataChange setInvestigator(String key, String investName) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.INVEST);
    retval.investOrig = (String)investigators_.get(key);
    retval.investKey = key;
    retval.investNew = investName;
    investigators_.put(key, investName);
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Merge all investigator refs to a single one
  */
  
  public PertDataChange mergeInvestigatorRefs(Set expsToMerge, String investKey, Set abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXPERIMENT_SET);
    retval.expSubsetOrig = new HashMap();
    retval.expSubsetNew = new HashMap();
    Iterator k2dit = expsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = (String)k2dit.next();
      Experiment exp = (Experiment)experiments_.get(key);
      List investList = exp.getInvestigators();
      boolean haveAChange = false;
      int numInv = investList.size();
      ArrayList replaceInvest = new ArrayList();
      for (int i = 0; i < numInv; i++) {
        String chkKey = (String)investList.get(i);
        boolean iChange = abandonKeys.contains(chkKey) && !chkKey.equals(investKey);
        if (iChange) {
          if (!replaceInvest.contains(investKey)) {
            replaceInvest.add(investKey);
          }
        } else {
          if (!replaceInvest.contains(chkKey)) {
            replaceInvest.add(chkKey);
          }
        }
        haveAChange = haveAChange || iChange;
      } 
      if (haveAChange) {
        retval.expSubsetOrig.put(key, exp.clone());
        exp.setInvestigators(replaceInvest);
        retval.expSubsetNew.put(key, exp.clone()); 
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Merge all source def refs to a single one
  */
  
  public PertDataChange mergeSourceDefRefs(Set expsToMerge, String sdefKey, Set abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXPERIMENT_SET);
    retval.expSubsetOrig = new HashMap();
    retval.expSubsetNew = new HashMap();
    Iterator k2dit = expsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = (String)k2dit.next();
      Experiment exp = (Experiment)experiments_.get(key);
      ArrayList replaceSources = new ArrayList();
      boolean haveAChange = false;
      PertSources pss = exp.getSources();
      Iterator pssit = pss.getSources();
      while (pssit.hasNext()) {
        String psid =(String)pssit.next();
        boolean iChange = abandonKeys.contains(psid) && !psid.equals(sdefKey);
        if (iChange) {
          if (!replaceSources.contains(sdefKey)) {
            replaceSources.add(sdefKey);
          }
        } else {
          if (!replaceSources.contains(psid)) {
            replaceSources.add(psid);
          }
        }
        haveAChange = haveAChange || iChange;
      }      
      if (haveAChange) {
        retval.expSubsetOrig.put(key, exp.clone());
        PertSources pss2 = new PertSources(replaceSources);
        exp.setSources(pss2);
        retval.expSubsetNew.put(key, exp.clone()); 
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }

  /***************************************************************************
  **
  ** Merge all experimental condition refs into one
  */
  
  public PertDataChange mergeExperimentCondRefs(Set expsToMerge, String condKey, Set abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXPERIMENT_SET);
    retval.expSubsetOrig = new HashMap();
    retval.expSubsetNew = new HashMap();
    Iterator k2dit = expsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = (String)k2dit.next();
      Experiment exp = (Experiment)experiments_.get(key);
      String currCond = exp.getConditionKey();
      if (!condKey.equals(currCond)) {
        retval.expSubsetOrig.put(key, exp.clone());
        exp.setConditionKey(condKey);
        retval.expSubsetNew.put(key, exp.clone()); 
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }

  /***************************************************************************
  **
  ** Merge all data point annotations to a single one
  */
  
  public PertDataChange mergeDataPointAnnotRefs(Set dpToMerge, String annotKey, Set abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.DATA_ANNOTS_SET);
    retval.dataPtNotesSubsetOrig = new HashMap();
    retval.dataPtNotesSubsetNew = new HashMap();
    Iterator dpkit = dpToMerge.iterator();
    while (dpkit.hasNext()) {
      String key = (String)dpkit.next();
      ArrayList annots = (ArrayList)dataPointNotes_.get(key);
      boolean haveAChange = false;
      int numA = annots.size();
      ArrayList replaceAnnots = new ArrayList();
      for (int i = 0; i < numA; i++) {
        String chkKey = (String)annots.get(i);
        boolean iChange = abandonKeys.contains(chkKey) && !chkKey.equals(annotKey);
        if (iChange) {
          if (!replaceAnnots.contains(annotKey)) {
            replaceAnnots.add(annotKey);
          }
        } else {
          if (!replaceAnnots.contains(chkKey)) {
            replaceAnnots.add(chkKey);
          }
        }
        haveAChange = haveAChange || iChange;
      } 
      if (haveAChange) {
        retval.dataPtNotesSubsetOrig.put(key, annots.clone());
        dataPointNotes_.put(key, replaceAnnots);
        retval.dataPtNotesSubsetNew.put(key, replaceAnnots.clone()); 
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Merge all target annotations to a single one
  */
  
  public PertDataChange mergeTargetAnnotRefs(Set trgToMerge, String annotKey, Set abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.TARG_ANNOTS_SET);
    retval.targetNotesSubsetOrig = new HashMap();
    retval.targetNotesSubsetNew = new HashMap();
    Iterator trgkit = trgToMerge.iterator();
    while (trgkit.hasNext()) {
      String key = (String)trgkit.next();
      ArrayList annots = (ArrayList)targetGeneNotes_.get(key);
      boolean haveAChange = false;
      int numA = annots.size();
      ArrayList replaceAnnots = new ArrayList();
      for (int i = 0; i < numA; i++) {
        String chkKey = (String)annots.get(i);
        boolean iChange = abandonKeys.contains(chkKey) && !chkKey.equals(annotKey);
        if (iChange) {
          if (!replaceAnnots.contains(annotKey)) {
            replaceAnnots.add(annotKey);
          }
        } else {
          if (!replaceAnnots.contains(chkKey)) {
            replaceAnnots.add(chkKey);
          }
        }
        haveAChange = haveAChange || iChange;
      } 
      if (haveAChange) {
        retval.targetNotesSubsetOrig.put(key, annots.clone());
        targetGeneNotes_.put(key, replaceAnnots);
        retval.targetNotesSubsetNew.put(key, replaceAnnots.clone()); 
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo a target annotation set change
  */
  
  private void targetAnnotSetUndo(PertDataChange undo) {
    Iterator dpnkit = undo.targetNotesSubsetOrig.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = (String)dpnkit.next();
      ArrayList dpn = (ArrayList)undo.targetNotesSubsetOrig.get(key);
      ArrayList dpnc = (ArrayList)dpn.clone();
      targetGeneNotes_.put(key, dpnc);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo target annotation change
  */
  
  private void targetAnnotSetRedo(PertDataChange redo) {
    Iterator dpnkit = redo.targetNotesSubsetNew.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = (String)dpnkit.next();
      ArrayList dpn = (ArrayList)redo.targetNotesSubsetNew.get(key);
      ArrayList dpnc = (ArrayList)dpn.clone();
      if (dpnc.isEmpty()) {  // Empty list means delete
        targetGeneNotes_.remove(key);
      } else {
        targetGeneNotes_.put(key, dpnc);
      }
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }
  
  /***************************************************************************
  **
  ** Merge all source def annotations to a single one
  */
  
  public PertDataChange mergeSourceDefAnnotRefs(Set sdToMerge, String annotKey, Set abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.SOURCE_DEF_SET);
    retval.srcDefsSubsetOrig = new HashMap();
    retval.srcDefsSubsetNew = new HashMap();
 
    Iterator sdit = sdToMerge.iterator();
    while (sdit.hasNext()) {
      String psdKey = (String)sdit.next();
      PertSource chk = (PertSource)sourceDefs_.get(psdKey);
      List annots = chk.getAnnotationIDs();
      boolean haveAChange = false;
      int numA = annots.size();
      ArrayList replaceAnnots = new ArrayList();
      for (int i = 0; i < numA; i++) {
        String chkKey = (String)annots.get(i);
        boolean iChange = abandonKeys.contains(chkKey) && !chkKey.equals(annotKey);
        if (iChange) {
          if (!replaceAnnots.contains(annotKey)) {
            replaceAnnots.add(annotKey);
          }
        } else {
          if (!replaceAnnots.contains(chkKey)) {
            replaceAnnots.add(chkKey);
          }
        }
        haveAChange = haveAChange || iChange;
      } 
      if (haveAChange) {
        retval.srcDefsSubsetOrig.put(psdKey, chk.clone());
        chk.setAnnotationIDs(replaceAnnots);
        retval.srcDefsSubsetNew.put(psdKey, chk.clone()); 
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Merge investigators.  Caller must handle merged references first!
  */
  
  public PertDataChange[] mergeInvestigatorNames(List keyIDs, String useKey, String newName) {
    ArrayList allRets = new ArrayList();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.INVEST);
      String keyID = (String)keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retval.investOrig = (String)investigators_.get(keyID);
        retval.investKey = keyID;
        retval.investNew = newName;
        investigators_.put(keyID, newName);
      } else {
        retval.investOrig = (String)investigators_.get(keyID);
        retval.investKey = keyID;
        retval.investNew = null;
        investigators_.remove(keyID);
        labels_.removeLabel(keyID);
      }
      retval.serialNumberNew = ++serialNumber_;
      allRets.add(retval);
    }    
    PertDataChange[] changes = (PertDataChange[])allRets.toArray(new PertDataChange[allRets.size()]);
    return (changes);
  }

  /***************************************************************************
  **
  ** Delete an investigator.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deleteInvestigator(String keyID) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.INVEST);
    retval.investKey = keyID;
    retval.investOrig = (String)investigators_.get(keyID);
    investigators_.remove(keyID);
    labels_.removeLabel(keyID);
    retval.investNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo an annotation
  */
  
  private void annotationUndo(PertDataChange undo) { 
    if (undo.annotTagOrig == null) { // Undo an add
      pertAnnot_.deleteMessage(undo.annotKey);
    } else if (undo.annotTagNew == null) {  // Undo a delete
      pertAnnot_.addMessageExistingKey(undo.annotKey, undo.annotTagOrig, undo.annotMsgOrig);
    } else {  // Change data
      pertAnnot_.editMessage(undo.annotKey, undo.annotTagOrig, undo.annotMsgOrig);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo an annotation.
  */
  
  private void annotationRedo(PertDataChange redo) {
    if (redo.annotTagOrig == null) { // Redo an add
      pertAnnot_.addMessageExistingKey(redo.annotKey, redo.annotTagNew, redo.annotMsgNew);
    } else if (redo.annotTagNew == null) {  // redo a delete
      pertAnnot_.deleteMessage(redo.annotKey);
    } else {  // Change data
      pertAnnot_.editMessage(redo.annotKey, redo.annotTagNew, redo.annotMsgNew);
    }
    serialNumber_ = redo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Add a new annotation. Caller must insure we have uniqueness first!
  */
  
  public PertDataChange addAnnotation(String tag, String message) { 
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.ANNOTATION);
    retval.annotMsgOrig = null;
    retval.annotTagOrig = null;
    retval.annotKey = pertAnnot_.addMessage(tag, message);
    retval.annotMsgNew = message;
    retval.annotTagNew = tag;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Edit an existing annotation. Caller must insure we have uniqueness first!
  */
  
  public PertDataChange editAnnotation(String id, String tag, String message) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.ANNOTATION);
    retval.annotKey = id;
    retval.annotMsgOrig = pertAnnot_.getMessage(id);
    retval.annotTagOrig = pertAnnot_.getTag(id);
    pertAnnot_.editMessage(id, tag, message);
    retval.annotMsgNew = message;
    retval.annotTagNew = tag;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete an existing annotation.
  */
  
  public PertDataChange deleteAnnotation(String id) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.ANNOTATION);
    retval.annotKey = id;
    retval.annotMsgOrig = pertAnnot_.getMessage(id);
    retval.annotTagOrig = pertAnnot_.getTag(id);
    pertAnnot_.deleteMessage(id);
    retval.annotMsgNew = null;
    retval.annotTagNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Merge annotations. Caller must insure we have uniqueness first!
  */
  
  public PertDataChange mergeAnnotations(List joinKeys, String commonKey, String tag, String message) { 
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.ANNOT_MODULE);
    retval.pAnnotOrig = (PertAnnotations)pertAnnot_.clone();
    pertAnnot_.mergeAnnotations(joinKeys, commonKey, tag, message);
    retval.pAnnotNew = (PertAnnotations)pertAnnot_.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo an annotation module change
  */
  
  private void annotModuleUndo(PertDataChange undo) {
    pertAnnot_ = undo.pAnnotOrig;
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo annotation module change
  */
  
  private void annotModuleRedo(PertDataChange redo) {
    pertAnnot_ = redo.pAnnotNew;
    serialNumber_ = redo.serialNumberNew;
    return;
  }

  /***************************************************************************
  **
  ** Target count:
  */
  
  public int getTargetCount() {
    return (targets_.size());
  }  
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  public String getTargetFromName(String targName) {
    String normName = DataUtil.normKey(targName);
    Iterator ikit = targets_.keySet().iterator();
    while (ikit.hasNext()) {
      String trgKey = (String)ikit.next();
      String trg = (String)targets_.get(trgKey);
      if (normName.equals(DataUtil.normKey(trg))) {
        return (trgKey);
      }
    }
    return (null);
  } 

  /***************************************************************************
  **
  ** Return, or create and return, a target that matches:
  */
  
  public KeyAndDataChange provideTarget(String targName) {
    String foundTarg = getTargetFromName(targName);
    if (foundTarg != null) {
      return (new KeyAndDataChange(foundTarg, null));
    }
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.TARGET_NAME);
    retval.targetOrig = null;
    String nextID = labels_.getNextLabel();   
    retval.targetKey = nextID;
    retval.targetNew = targName;
    targets_.put(nextID, targName);
    retval.serialNumberNew = ++serialNumber_;
    return (new KeyAndDataChange(nextID, retval));
  }
  
  /***************************************************************************
  **
  ** Add an investigator via IO:
  */
  
  public void addInvestForIO(Invest invest) {
    investigators_.put(invest.id, invest.name);
    labels_.addExistingLabel(invest.id);
    return;
  }
  
  /***************************************************************************
  **
  ** Add an source name via IO:
  */
  
  public void addSourceNameForIO(SourceName sn) {
    sourceNames_.put(sn.id, sn.name);
    labels_.addExistingLabel(sn.id);
    return;
  }

  /***************************************************************************
  **
  ** Add a target via IO:
  */
  
  public void addTargetForIO(AugTarget augt) {
    targets_.put(augt.id, augt.name);
    labels_.addExistingLabel(augt.id);
    if (augt.notes != null) {
      setFootnotesForTargetIO(augt.id, augt.notes);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** For IO
  */
  
  void addPertSrcInfoForIO(Experiment psi) {
    experiments_.put(psi.getID(), psi);
    labels_.addExistingLabel(psi.getID());
    return;
  }
  
  /***************************************************************************
  **
  ** For IO
  */
  
  void addPertSrcDefForIO(PertSource ps) {
    sourceDefs_.put(ps.getID(), ps);
    labels_.addExistingLabel(ps.getID());
    return;
  }
 
  /***************************************************************************
  **
  ** Add or replace a target name, along with annotations
  */
  
  public PertDataChange[] setTargetName(String keyID, String name, List annots) {
    return (setTargetNameManageIdents(keyID, name, annots, true));
  }
  
  /***************************************************************************
  **
  ** Add or replace a target name, along with annotations
  */
  
  private PertDataChange[] setTargetNameManageIdents(String keyID, String name, List annots, boolean doIdents) {
    ArrayList retlist = new ArrayList();

    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.TARGET_NAME);
    retval.targetKey = keyID;
    retval.targetOrig = (String)targets_.get(keyID);
    targets_.put(keyID, name); 
    retval.targetNew = name;
    retval.serialNumberNew = ++serialNumber_;
    retlist.add(retval);
      
    PertDataChange fnpdc = setFootnotesForTarget(keyID, annots);
    if (fnpdc != null) {
      retlist.add(fnpdc);
    }
    
    if (doIdents) {
      retlist.addAll(Arrays.asList(entryMap_.dropIdentityMaps(targets_)));
    }
        
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Add or replace a target name, along with annotations
  */
  
  private PertDataChange[] changeTargetNameManageIdents(String keyID, String name) {
    ArrayList retlist = new ArrayList();

    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.TARGET_NAME);
    retval.targetKey = keyID;
    retval.targetOrig = (String)targets_.get(keyID);
    targets_.put(keyID, name); 
    retval.targetNew = name;
    retval.serialNumberNew = ++serialNumber_;
    retlist.add(retval);      
    retlist.addAll(Arrays.asList(entryMap_.dropIdentityMaps(targets_)));        
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }

  /***************************************************************************
  **
  ** Delete a target name.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange[] deleteTargetName(String keyID) {
    ArrayList retlist = new ArrayList();
     
    PertDataChange fnpdc = setFootnotesForTarget(keyID, null);
    if (fnpdc != null) {
      retlist.add(fnpdc);
    }
      
    PertDataChange[] vals = entryMap_.dropDanglingMapsFor(keyID);
    for (int i = 0; i < vals.length; i++) {
      retlist.add(vals[i]);
    } 
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.TARGET_NAME);
    retval.targetKey = keyID;
    retval.targetOrig = (String)targets_.get(keyID);
    targets_.remove(keyID);
    labels_.removeLabel(keyID);
    retval.targetNew = null;
    retval.serialNumberNew = ++serialNumber_;
    retlist.add(retval);
   
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  
  /***************************************************************************
  **
  ** Merge a bunch of target names.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergeTargetNames(List keyIDs, String useKey, String newName, List newFoots) {
    ArrayList retlist = new ArrayList();
    
    retlist.addAll(Arrays.asList(entryMap_.mergeMapsFor(useKey, keyIDs)));

    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = (String)keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.addAll(Arrays.asList(setTargetNameManageIdents(useKey, newName, newFoots, false)));
      } else {
        retlist.addAll(Arrays.asList(deleteTargetName(keyID)));
      }
    }
    
    retlist.addAll(Arrays.asList(entryMap_.dropIdentityMaps(targets_)));
     
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge a bunch of experimental controls.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergeExprControls(List keyIDs, String useKey, ExperimentControl revisedECtrl) {
    ArrayList retlist = new ArrayList();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = (String)keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setExperimentControl(revisedECtrl));
      } else {
        retlist.add(deleteExperimentControl(keyID));
      }
    }
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge a bunch of measurement props.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergeMeasureProps(List keyIDs, String useKey, MeasureProps revisedMp) {
    ArrayList retlist = new ArrayList();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = (String)keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setMeasureProp(revisedMp));        
      } else {
        retlist.add(deleteMeasureProp(keyID));    
      }
    }
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge a bunch of perturbation properties.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergePertProps(List keyIDs, String useKey, PertProperties revisedPp) {
    ArrayList retlist = new ArrayList();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = (String)keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setPerturbationProp(revisedPp));  
      } else {
        retlist.add(deletePerturbationProp(keyID));
      }
    }
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge a bunch of experimental conditions.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergeExprConditions(List keyIDs, String useKey, ExperimentConditions revisedECond) {
    ArrayList retlist = new ArrayList();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = (String)keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setExperimentConditions(revisedECond));
      } else {
        retlist.add(deleteExperimentConditions(keyID));
      }
    }
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }

  /***************************************************************************
  **
  ** Merge a bunch of source names.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergeSourceNames(List keyIDs, String useKey, String newName) {
    ArrayList retlist = new ArrayList();
     
    // All nodes that have a custom map to one of the merged sources will now
    // map to the single merged source:
    //
    
    retlist.addAll(Arrays.asList(sourceMap_.mergeMapsFor(useKey, keyIDs)));
      
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = (String)keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.addAll(Arrays.asList(setSourceNameManageIdents(useKey, newName, false)));
      } else {
        retlist.addAll(Arrays.asList(deleteSourceName(keyID)));
      }
    }
    
    retlist.addAll(Arrays.asList(sourceMap_.dropIdentityMaps(sourceNames_)));
    
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }

  
  /***************************************************************************
  **
  ** Add or replace a source name
  */
  
  public PertDataChange[] setSourceName(String keyID, String name) {
    return (setSourceNameManageIdents(keyID, name, true));
  }
  
  /***************************************************************************
  **
  ** Add or replace a source name
  */
  
  private PertDataChange[] setSourceNameManageIdents(String keyID, String name, boolean dropIdent) {
    ArrayList retlist = new ArrayList();
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.SOURCE_NAME);
    retval.srcNameKey = keyID;
    retval.srcNameOrig = sourceNames_.get(keyID);
    sourceNames_.put(keyID, name); 
    retval.srcNameNew = name;
    retval.serialNumberNew = ++serialNumber_;
    retlist.add(retval);
    
    if (dropIdent) {
      retlist.addAll(Arrays.asList(sourceMap_.dropIdentityMaps(sourceNames_)));
    }
    
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }
   
  /***************************************************************************
  **
  ** Duplicate a source name map (if it exists)
  */
  
  public PertDataChange[] duplicateSourceNameMap(String existingID, String dupID) {
    return (sourceMap_.dupAMapTo(existingID, dupID));
  }

  /***************************************************************************
  **
  ** Duplicate a target name map (if it exists)
  */
  
  public PertDataChange[] duplicateTargetNameMap(String existingID, String dupID) {
    return (entryMap_.dupAMapTo(existingID, dupID));
  }
  
  /***************************************************************************
  **
  ** Answer if we have a map to the source
  */
  
  public boolean haveSourceNameMapTo(String chkID) {
    return (sourceMap_.haveMapToEntry(chkID));
  }

  /***************************************************************************
  **
  ** Answer if we have a map to the target
  */
  
  public boolean haveTargetNameMapTo(String chkID) {
    return (entryMap_.haveMapToEntry(chkID));
  }

  /***************************************************************************
  **
  ** Delete a source name.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange[] deleteSourceName(String keyID) {
    ArrayList retlist = new ArrayList();
     
    PertDataChange[] vals = sourceMap_.dropDanglingMapsFor(keyID);
    for (int i = 0; i < vals.length; i++) {
      retlist.add(vals[i]);
    } 

    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.SOURCE_NAME);
    retval.srcNameKey = keyID;
    retval.srcNameOrig = sourceNames_.get(keyID);
    sourceNames_.remove(keyID);
    labels_.removeLabel(keyID);
    retval.srcNameNew = null;
    retval.serialNumberNew = ++serialNumber_;
    retlist.add(retval);
    
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Add or replace a measurement property
  */
  
  public PertDataChange setMeasureProp(MeasureProps mprops) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.MEASURE_PROP);
    String key = mprops.getID();  
    MeasureProps orig = measureDict_.getMeasureProps(key);
    retval.mpOrig = (orig == null) ? null : (MeasureProps)orig.clone();
    measureDict_.setMeasureProp(mprops);
    retval.mpNew = (MeasureProps)mprops.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete a measurement property.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deleteMeasureProp(String key) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.MEASURE_PROP);
    MeasureProps orig = measureDict_.getMeasureProps(key);
    retval.mpOrig = (MeasureProps)orig.clone();
    measureDict_.dropMeasureProp(key);
    retval.mpNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo measurement property operation:
  */
  
  private void measurePropUndo(PertDataChange undo) { 
    if (undo.mpOrig == null) { // Undo an add
      String rescueID = undo.mpNew.getID();
      measureDict_.dropMeasureProp(rescueID);
    } else if (undo.mpNew == null) {  // Undo a delete
      MeasureProps mpo = (MeasureProps)undo.mpOrig.clone();
      measureDict_.addMeasurePropForIO(mpo);  // Handles ID accounting
    } else {  // Change data
      MeasureProps mpo = (MeasureProps)undo.mpOrig.clone();
      measureDict_.setMeasureProp(mpo);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo measurement property operation:
  */
  
  private void measurePropRedo(PertDataChange redo) { 
    if (redo.mpOrig == null) { // Redo an add
      MeasureProps mpn = (MeasureProps)redo.mpNew.clone();
      measureDict_.addMeasurePropForIO(mpn);  // Handles ID accounting
    } else if (redo.mpNew == null) {  // Redo a delete
      String rescueID = redo.mpOrig.getID();
      measureDict_.dropMeasureProp(rescueID);
    } else {  // Change data
      MeasureProps mpn = (MeasureProps)redo.mpNew.clone();
      measureDict_.setMeasureProp(mpn); 
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 
    
  /***************************************************************************
  **
  ** Add or replace an experimental condition
  */
  
  public PertDataChange setExperimentConditions(ExperimentConditions expCond) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXP_COND);
    String key = expCond.getID();  
    ExperimentConditions orig = condDict_.getExprConditions(key);
    retval.ecOrig = (orig == null) ? null : (ExperimentConditions)orig.clone();
    condDict_.setExprCondition(expCond);
    retval.ecNew = (ExperimentConditions)expCond.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Delete an experimental condition.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deleteExperimentConditions(String key) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXP_COND);
    ExperimentConditions orig = condDict_.getExprConditions(key);
    retval.ecOrig = (ExperimentConditions)orig.clone();
    condDict_.dropExprConditions(key);
    retval.ecNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo experimental condition operation:
  */
  
  private void expCondUndo(PertDataChange undo) { 
    if (undo.ecOrig == null) { // Undo an add
      String rescueID = undo.ecNew.getID();
      condDict_.dropExprConditions(rescueID);
    } else if (undo.ecNew == null) {  // Undo a delete
      ExperimentConditions eco = (ExperimentConditions)undo.ecOrig.clone();
      condDict_.addExprConditionsForIO(eco);  // Handles ID accounting
    } else {  // Change data
      ExperimentConditions eco = (ExperimentConditions)undo.ecOrig.clone();
      condDict_.setExprCondition(eco);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo experimental condition operation:
  */
  
  private void expCondRedo(PertDataChange redo) { 
    if (redo.ecOrig == null) { // Redo an add
      ExperimentConditions ecn = (ExperimentConditions)redo.ecNew.clone();
      condDict_.addExprConditionsForIO(ecn);  // Handles ID accounting
    } else if (redo.ecNew == null) {  // Redo a delete
      String rescueID = redo.ecOrig.getID();
      condDict_.dropExprConditions(rescueID);
    } else {  // Change data
      ExperimentConditions ecn = (ExperimentConditions)redo.ecNew.clone();
      condDict_.setExprCondition(ecn); 
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 
  
  /***************************************************************************
  **
  ** Add or replace an experimental control
  */
  
  public PertDataChange setExperimentControl(ExperimentControl expCtrl) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXP_CONTROL);
    String key = expCtrl.getID();  
    ExperimentControl orig = condDict_.getExprControl(key);
    retval.ectrlOrig = (orig == null) ? null : (ExperimentControl)orig.clone();
    condDict_.setExprControl(expCtrl);
    retval.ectrlNew = (ExperimentControl)expCtrl.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Delete an experimental control.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deleteExperimentControl(String key) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXP_CONTROL);
    ExperimentControl orig = condDict_.getExprControl(key);
    retval.ectrlOrig = (ExperimentControl)orig.clone();
    condDict_.dropExprControl(key);
    retval.ectrlNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo experimental control operation:
  */
  
  private void expControlUndo(PertDataChange undo) { 
    if (undo.ectrlOrig == null) { // Undo an add
      String rescueID = undo.ectrlNew.getID();
      condDict_.dropExprControl(rescueID);
    } else if (undo.ectrlNew == null) {  // Undo a delete
      ExperimentControl eco = (ExperimentControl)undo.ectrlOrig.clone();
      condDict_.addExprControlForIO(eco);  // Handles ID accounting
    } else {  // Change data
      ExperimentControl eco = (ExperimentControl)undo.ectrlOrig.clone();
      condDict_.setExprControl(eco);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo experimental control operation:
  */
  
  private void expControlRedo(PertDataChange redo) { 
    if (redo.ectrlOrig == null) { // Redo an add
      ExperimentControl ecn = (ExperimentControl)redo.ectrlNew.clone();
      condDict_.addExprControlForIO(ecn);  // Handles ID accounting
    } else if (redo.ectrlNew == null) {  // Redo a delete
      String rescueID = redo.ectrlOrig.getID();
      condDict_.dropExprControl(rescueID);
    } else {  // Change data
      ExperimentControl ecn = (ExperimentControl)redo.ectrlNew.clone();
      condDict_.setExprControl(ecn); 
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 
   
  /***************************************************************************
  **
  ** Add or replace a measurement scale
  */
  
  public PertDataChange setMeasureScale(MeasureScale mScale) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.MEAS_SCALE);
    String key = mScale.getID();  
    MeasureScale orig = measureDict_.getMeasureScale(key);
    retval.mScaleOrig = (orig == null) ? null : (MeasureScale)orig.clone();
    measureDict_.setMeasureScale(mScale);
    retval.mScaleNew = (MeasureScale)mScale.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Delete a measurement scale.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deleteMeasureScale(String key) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.MEAS_SCALE);
    MeasureScale orig = measureDict_.getMeasureScale(key);
    retval.mScaleOrig = (MeasureScale)orig.clone();
    measureDict_.dropMeasureScale(key);
    retval.mScaleNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo measurement scale operation:
  */
  
  private void measureScaleUndo(PertDataChange undo) { 
    if (undo.mScaleOrig == null) { // Undo an add
      String rescueID = undo.mScaleNew.getID();
      measureDict_.dropMeasureScale(rescueID);
    } else if (undo.mScaleNew == null) {  // Undo a delete
      MeasureScale mso = (MeasureScale)undo.mScaleOrig.clone();
      measureDict_.addMeasureScaleForIO(mso);  // Handles ID accounting
    } else {  // Change data
      MeasureScale mso = (MeasureScale)undo.mScaleOrig.clone();
      measureDict_.setMeasureScale(mso);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo measurement scale operation:
  */
  
  private void measureScaleRedo(PertDataChange redo) { 
    if (redo.mScaleOrig == null) { // Redo an add
      MeasureScale msn = (MeasureScale)redo.mScaleNew.clone();
      measureDict_.addMeasureScaleForIO(msn);  // Handles ID accounting
    } else if (redo.mScaleNew == null) {  // Redo a delete
      String rescueID = redo.mScaleOrig.getID();
      measureDict_.dropMeasureScale(rescueID);
    } else {  // Change data
      MeasureScale msn = (MeasureScale)redo.mScaleNew.clone();
      measureDict_.setMeasureScale(msn); 
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 

  /***************************************************************************
  **
  ** Merge all measurement scales refs to a single one
  */
  
  public PertDataChange mergeMeasureScaleRefs(Set mPropsToMerge, String scaleKey) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.MEASURE_PROP_SET);
    retval.mPropsSubsetOrig = new HashMap();
    retval.mPropsSubsetNew = new HashMap();
    Iterator k2dit = mPropsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = (String)k2dit.next();
      MeasureProps mp = measureDict_.getMeasureProps(key);
      if (!scaleKey.equals(mp.getScaleKey())) {
        retval.mPropsSubsetOrig.put(key, mp.clone());
        mp = (MeasureProps)mp.clone();
        mp.setScaleKey(scaleKey);
        measureDict_.setMeasureProp(mp);
        retval.mPropsSubsetNew.put(key, mp.clone());
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }

    
 /***************************************************************************
  **
  ** Undo measure prop set change
  */
  
  private void measurePropSetUndo(PertDataChange undo) {
    Iterator dpnkit = undo.mPropsSubsetOrig.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = (String)dpnkit.next();
      MeasureProps mp = (MeasureProps)undo.mPropsSubsetOrig.get(key);
      MeasureProps mpc = (MeasureProps)mp.clone();
      measureDict_.setMeasureProp(mpc);
    }    
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo measure prop set change
  */
  
  private void measurePropSetRedo(PertDataChange redo) {
    Iterator dpnkit = redo.mPropsSubsetNew.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = (String)dpnkit.next();
      MeasureProps mp = (MeasureProps)redo.mPropsSubsetNew.get(key);
      MeasureProps mpc = (MeasureProps)mp.clone();
      measureDict_.setMeasureProp(mpc);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }     

  /***************************************************************************
  **
  ** Merge measurement scales.  Caller must handle merged references first!
  */
  
  public PertDataChange[] mergeMeasureScales(List keyIDs, String useKey, MeasureScale scale) {
    ArrayList retlist = new ArrayList();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = (String)keyIDs.get(i);
      if (keyID.equals(useKey)) {
        if (!scale.getID().equals(useKey)) {
          throw new IllegalArgumentException();
        }
        retlist.add(setMeasureScale((MeasureScale)scale.clone()));
      } else {
        retlist.add(deleteMeasureScale(keyID));
      }
    }
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }

  /***************************************************************************
  **
  ** Add or replace a perturbation property
  */
  
  public PertDataChange setPerturbationProp(PertProperties pprops) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.PERT_PROP);
    String key = pprops.getID();  
    PertProperties orig = pertDict_.getPerturbProps(key);
    retval.ppOrig = (orig == null) ? null : (PertProperties)orig.clone();
    pertDict_.setPerturbProp(pprops);
    retval.ppNew = (PertProperties)pprops.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete a perturbation property.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deletePerturbationProp(String key) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.PERT_PROP);
    PertProperties orig = pertDict_.getPerturbProps(key);
    retval.ppOrig = (PertProperties)orig.clone();
    pertDict_.dropPerturbProp(key);
    retval.ppNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }

  /***************************************************************************
  **
  ** Clone
  */

  public Object clone() {
    try {
      PerturbationData newVal = (PerturbationData)super.clone();
      
      newVal.dataPoints_ = new HashMap();
      Iterator dpit = this.dataPoints_.keySet().iterator();
      while (dpit.hasNext()) {
        String dpKey = (String)dpit.next();
        newVal.dataPoints_.put(dpKey, this.dataPoints_.get(dpKey).clone());
      }
      
      newVal.sourceDefs_ = new HashMap();
      Iterator sdit = this.sourceDefs_.keySet().iterator();
      while (sdit.hasNext()) {
        String sdKey = (String)sdit.next();
        newVal.sourceDefs_.put(sdKey, ((PertSource)this.sourceDefs_.get(sdKey)).clone());
      }
      
      newVal.experiments_ = new HashMap();
      Iterator psit = this.experiments_.keySet().iterator();
      while (psit.hasNext()) {
        String psKey = (String)psit.next();
        newVal.experiments_.put(psKey, this.experiments_.get(psKey).clone());
      }
      
      newVal.dataPointNotes_ = new HashMap();
      Iterator dpnit = this.dataPointNotes_.keySet().iterator();
      while (dpnit.hasNext()) {
        String dpKey = (String)dpnit.next();
        newVal.dataPointNotes_.put(dpKey, new ArrayList<String>(this.dataPointNotes_.get(dpKey)));
      } 
      
      newVal.userData_ = new HashMap();
      Iterator udit = this.userData_.keySet().iterator();
      while (udit.hasNext()) {
        String udKey = (String)udit.next();
        // Data is immutable, shallow copy is OK:
        newVal.userData_.put(udKey, new ArrayList<String>(this.dataPointNotes_.get(udKey)));
      } 
 
      newVal.pertAnnot_ = (PertAnnotations)this.pertAnnot_.clone();
      newVal.labels_ = (UniqueLabeller)this.labels_.clone();
      
      newVal.entryMap_ = (NameMapper)this.entryMap_.clone();
      newVal.sourceMap_ = (NameMapper)this.sourceMap_.clone();
      
      newVal.investigators_ = (HashMap)this.investigators_.clone();
      newVal.targets_ = (HashMap)this.targets_.clone();
      newVal.sourceNames_ = (HashMap)this.sourceNames_.clone();

      newVal.regionRestrictions_ = new HashMap();
      Iterator rrit = this.regionRestrictions_.keySet().iterator();
      while (rrit.hasNext()) {
        String rKey = (String)rrit.next();
        newVal.regionRestrictions_.put(rKey, this.regionRestrictions_.get(rKey).clone());
      }
  
      newVal.pertDict_ = (PertDictionary)this.pertDict_.clone();
      newVal.measureDict_ = (MeasureDictionary)this.measureDict_.clone();
      newVal.condDict_ = (ConditionDictionary)this.condDict_.clone();
      newVal.userFields_ = (ArrayList)this.userFields_.clone();
 
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }
 
  /***************************************************************************
  **
  ** Get a choice for a target name
  */
  
  public TrueObjChoiceContent getTargetChoiceContent(String key, boolean showAnnots) {
    String targ = (showAnnots) ? getAnnotatedTargetDisplay(key) : (String)targets_.get(key);  
    return (new TrueObjChoiceContent(targ, key));    
  }
  
  /***************************************************************************
  **
  ** Get a choice for a source or proxy name
  */
  
  public TrueObjChoiceContent getSourceOrProxyNameChoiceContent(String key) {
    String sname = sourceNames_.get(key);
    return (new TrueObjChoiceContent(sname, key));
  }
  
  /***************************************************************************
  **
  ** Get a choice for an investigator
  */
  
  public TrueObjChoiceContent getInvestigatorChoiceContent(String key) {
    String invest = (String)investigators_.get(key);
    return (new TrueObjChoiceContent(invest, key));    
  }
  
  /***************************************************************************
  **
  ** Get ALL the candidate values for the given single filter category (not
  ** just those present in a given set of data points)
  */
  
  public SortedSet getAllAvailableCandidates(int filterCat) {
    TreeSet retval = new TreeSet();
    switch (filterCat) {
      case PertFilter.EXPERIMENT:
        Iterator prsit = experiments_.values().iterator();
        while (prsit.hasNext()) {
          Experiment chk = (Experiment)prsit.next();
          chk.addToExperimentSet(retval, this);
        }
        return (retval);
      case PertFilter.TARGET:
        Iterator tkit = targets_.keySet().iterator();
        while (tkit.hasNext()) {
          String tkey = (String)tkit.next();
          retval.add(getTargetChoiceContent(tkey, true));
        }
        return (retval);    
      case PertFilter.SOURCE_OR_PROXY_NAME:
        Iterator snkit = sourceNames_.keySet().iterator();
        while (snkit.hasNext()) {
          String snkey = (String)snkit.next();
          retval.add(getSourceOrProxyNameChoiceContent(snkey));
        }
        return (retval);      
      case PertFilter.INVEST:
        Iterator invkit = investigators_.keySet().iterator();
        while (invkit.hasNext()) {
          String ikey = (String)invkit.next();
          retval.add(getInvestigatorChoiceContent(ikey));
        }
        return (retval); 
      case PertFilter.SOURCE_NAME:
      case PertFilter.SOURCE:
      case PertFilter.PERT:
      case PertFilter.TIME:
      case PertFilter.VALUE:
      case PertFilter.INVEST_LIST:
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
  ** Get the candidate values for the given single filter category
  */
  
  public SortedSet getCandidates(List chooseFrom, int filterCat) {
    TreeSet retval = new TreeSet();
    if (chooseFrom == null) {
      chooseFrom = new ArrayList(dataPoints_.values());
    }
    int numRs = chooseFrom.size();
    for (int i = 0; i < numRs; i++) {
      PertDataPoint pdp = (PertDataPoint)chooseFrom.get(i);
      pdp.getCandidates(appState_, filterCat, retval, this);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the candidate values for the given single filter category
  */
  
  public SortedSet getCandidates(int filterCat) {
    return (getCandidates(null, filterCat));
  }
   
  /***************************************************************************
  **
  ** Get all the perturbation data as filtered
  */
  
  public List<PertDataPoint> getPerturbations(PertFilterExpression pfe) {
    TreeMap<String, PertFilterTarget> sorted = new TreeMap<String, PertFilterTarget>(dataPoints_);
    TreeSet sortedKeys = new TreeSet(sorted.keySet());
    
    ArrayList<PertDataPoint> retval = new ArrayList<PertDataPoint>();
    SortedSet<String> result = pfe.getFilteredResult(sortedKeys, sorted, this);
    Iterator resIt = result.iterator();
    while (resIt.hasNext()) {
      String pdpID = (String)resIt.next();
      retval.add((PertDataPoint)sorted.get(pdpID));
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the keys to the source definitions:
  */ 
  
  public Iterator<String> getSourceDefKeys() {
    return (sourceDefs_.keySet().iterator());
  }
  
  /***************************************************************************
  **
  ** Get the given source definition:
  */ 
  
  public PertSource getSourceDef(String key) {
    return ((PertSource)sourceDefs_.get(key));
  }

  /***************************************************************************
  **
  ** Get the annotations for a data point
  */ 
  
  public List getDataPointNotes(String dpkey) {
    return ((List)dataPointNotes_.get(dpkey));
  }
  
  /***************************************************************************
  **
  ** Get all keys for dp notes
  */ 
  
  public Iterator getDataPointNoteKeys() {
    return (dataPointNotes_.keySet().iterator());
  }
  
  /***************************************************************************
  **
  ** Add a data point
  */
  
  public void addDataPointForIO(PertDataPoint meas) {
    dataPoints_.put(meas.getID(), meas);
    labels_.addExistingLabel(meas.getID());
    return;
  }
    
  /***************************************************************************
  **
  ** Get an iterator over the data points
  */
  
  public Iterator getDataPoints() {
    return (dataPoints_.values().iterator());
  }
  
  /***************************************************************************
  **
  ** Get the count of measurements
  */
  
  public int getDataPointCount() {
    return (dataPoints_.size());
  }
   
  /***************************************************************************
  **
  ** Undo a data point set change
  */
  
  private void dataPointSetUndo(PertDataChange undo) {
    if (undo.dataPtsSubsetOrig == null) {
      throw new IllegalStateException();
    } else if (undo.dataPtsSubsetNew == null) {  // Undo a delete
      Iterator kit = undo.dataPtsSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = (String)kit.next();
        PertDataPoint pdp = (PertDataPoint)undo.dataPtsSubsetOrig.get(key);
        PertDataPoint pdpc = (PertDataPoint)pdp.clone();
        dataPoints_.put(key, pdpc);
        labels_.addExistingLabel(key);
      }
      // Restore region restrictions:
      if (undo.dataPtRegResSubsetOrig != null) {
        Iterator rrkit = undo.dataPtRegResSubsetOrig.keySet().iterator();
        while (rrkit.hasNext()) {
          String key = (String)rrkit.next();
          RegionRestrict rr = (RegionRestrict)undo.dataPtRegResSubsetOrig.get(key);
          RegionRestrict rrc = (RegionRestrict)rr.clone();
          regionRestrictions_.put(key, rrc);
        }
      }
      // Restore user data:
      if (undo.userDataSubsetOrig != null) {
        Iterator udkit = undo.userDataSubsetOrig.keySet().iterator();
        while (udkit.hasNext()) {
          String key = (String)udkit.next();
          ArrayList ud = (ArrayList)undo.userDataSubsetOrig.get(key);
          ArrayList udc = (ArrayList)ud.clone();
          userData_.put(key, udc);
        }
      }
      //  Restore note list:
      if (undo.dataPtNotesSubsetOrig != null) {
        Iterator dpnkit = undo.dataPtNotesSubsetOrig.keySet().iterator();
        while (dpnkit.hasNext()) {
          String key = (String)dpnkit.next();
          ArrayList dpn = (ArrayList)undo.dataPtNotesSubsetOrig.get(key);
          ArrayList dpnc = (ArrayList)dpn.clone();
          dataPointNotes_.put(key, dpnc);
        }
      }
  
    } else {  // Change data
      Iterator kit = undo.dataPtsSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = (String)kit.next();
        PertDataPoint pdp = (PertDataPoint)undo.dataPtsSubsetOrig.get(key);
        PertDataPoint pdpc = (PertDataPoint)pdp.clone();
        dataPoints_.put(key, pdpc);
      }
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a data point set change
  */
  
  private void dataPointSetRedo(PertDataChange redo) {
     if (redo.dataPtsSubsetOrig == null) {
      throw new IllegalStateException();
    } else if (redo.dataPtsSubsetNew == null) {  // redo a delete
      Iterator kit = redo.dataPtsSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = (String)kit.next();
        dataPoints_.remove(key);
        labels_.removeLabel(key);
      }
      // Delete region restrictions:
      if (redo.dataPtRegResSubsetOrig != null) {
        Iterator rrkit = redo.dataPtRegResSubsetOrig.keySet().iterator();
        while (rrkit.hasNext()) {
          String key = (String)rrkit.next();
          regionRestrictions_.remove(key);
        }
      }
      // Delete user data:
      if (redo.userDataSubsetOrig != null) {
        Iterator udkit = redo.userDataSubsetOrig.keySet().iterator();
        while (udkit.hasNext()) {
          String key = (String)udkit.next();
          userData_.remove(key);
        }
      }
      //  Delete note list:
      if (redo.dataPtNotesSubsetOrig != null) {
        Iterator dpnkit = redo.dataPtNotesSubsetOrig.keySet().iterator();
        while (dpnkit.hasNext()) {
          String key = (String)dpnkit.next();
          dataPointNotes_.remove(key);
        }
      } 
    } else {  // Redo Change data
      Iterator kit = redo.dataPtsSubsetNew.keySet().iterator();
      while (kit.hasNext()) {
        String key = (String)kit.next();
        PertDataPoint pdp = (PertDataPoint)redo.dataPtsSubsetNew.get(key);
        PertDataPoint pdpc = (PertDataPoint)pdp.clone();
        dataPoints_.put(key, pdpc);
      }
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }
  
  /***************************************************************************
  **
  ** Drop a bunch of data points
  */
  
  public PertDataChange deleteDataPoints(Set keysToDrop) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.DATA_POINT_SET);
    retval.dataPtsSubsetOrig = new HashMap();
    Iterator k2dit = keysToDrop.iterator();
    while (k2dit.hasNext()) {
      String key = (String)k2dit.next();
      // Delete region restrict:
      RegionRestrict regReg = (RegionRestrict)regionRestrictions_.get(key); 
      if (regReg != null) {
        if (retval.dataPtRegResSubsetOrig == null) {
          retval.dataPtRegResSubsetOrig = new HashMap();
        }
        retval.dataPtRegResSubsetOrig.put(key, regReg.clone());
        regionRestrictions_.remove(key);
      }
      // Delete user data:
      ArrayList userDataList = (ArrayList)userData_.get(key); 
      if (userDataList != null) {
        if (retval.userDataSubsetOrig == null) {
          retval.userDataSubsetOrig = new HashMap();
        }
        retval.userDataSubsetOrig.put(key, new ArrayList<String>(userDataList));
        userData_.remove(key);
      }
      // Delete note list:
      ArrayList noteList = (ArrayList)dataPointNotes_.get(key); 
      if (noteList != null) {
        if (retval.dataPtNotesSubsetOrig == null) {
          retval.dataPtNotesSubsetOrig = new HashMap();
        }
        retval.dataPtNotesSubsetOrig.put(key, noteList.clone());
        dataPointNotes_.remove(key);
      }
      // Delete the data:  
      PertDataPoint pdp = (PertDataPoint)dataPoints_.get(key);
      retval.dataPtsSubsetOrig.put(key, pdp.clone());
      dataPoints_.remove(key);
      labels_.removeLabel(key);
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Merge all data points to a single target
  */
  
  public PertDataChange mergeDataPointTargetRefs(Set pointsToMerge, String targKey) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.DATA_POINT_SET);
    retval.dataPtsSubsetOrig = new HashMap();
    retval.dataPtsSubsetNew = new HashMap();
    Iterator k2dit = pointsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = (String)k2dit.next();
      PertDataPoint pdp = (PertDataPoint)dataPoints_.get(key);
      if (!pdp.getTargetKey().equals(targKey)) {
        retval.dataPtsSubsetOrig.put(key, pdp.clone());
        pdp.setTargetKey(targKey);
        retval.dataPtsSubsetNew.put(key, pdp.clone());
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Merge all data points to a single control
  */
  
  public PertDataChange mergeDataPointControlRefs(Set pointsToMerge, String ctrlKey) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.DATA_POINT_SET);
    retval.dataPtsSubsetOrig = new HashMap();
    retval.dataPtsSubsetNew = new HashMap();
    Iterator k2dit = pointsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = (String)k2dit.next();
      PertDataPoint pdp = (PertDataPoint)dataPoints_.get(key);
      if (!pdp.getControl().equals(ctrlKey)) {
        retval.dataPtsSubsetOrig.put(key, pdp.clone());
        pdp.setControl(ctrlKey);
        retval.dataPtsSubsetNew.put(key, pdp.clone());
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }  

  /***************************************************************************
  **
  ** Merge all data points to a measurement prop
  */
  
  public PertDataChange mergeMeasurePropRefs(Set pointsToMerge, String mpKey) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.DATA_POINT_SET);
    retval.dataPtsSubsetOrig = new HashMap();
    retval.dataPtsSubsetNew = new HashMap();
    Iterator k2dit = pointsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = (String)k2dit.next();
      PertDataPoint pdp = (PertDataPoint)dataPoints_.get(key);
      if (!pdp.getMeasurementTypeKey().equals(mpKey)) {
        retval.dataPtsSubsetOrig.put(key, pdp.clone());
        pdp.setMeasurementTypeKey(mpKey);
        retval.dataPtsSubsetNew.put(key, pdp.clone());
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }  

  /***************************************************************************
  **
  ** Drop a bunch of experiments
  */
  
  public PertDataChange deleteExperiments(Set keysToDrop) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXPERIMENT_SET);    
    retval.expSubsetOrig = new HashMap();
    Iterator k2dit = keysToDrop.iterator();
    while (k2dit.hasNext()) {
      String dropkey = (String)k2dit.next();
      Experiment psi = (Experiment)experiments_.get(dropkey);
      retval.expSubsetOrig.put(dropkey, psi.clone());      
      experiments_.remove(dropkey);
      labels_.removeLabel(dropkey);
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Drop an investigator from a set of experiments
  */
  
  public PertDataChange dropInvestigator(String investID, Set keysToPrune) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXPERIMENT_SET);   
    retval.expSubsetOrig = new HashMap();
    retval.expSubsetNew = new HashMap();
    Iterator k2dit = keysToPrune.iterator();
    while (k2dit.hasNext()) {
      String pruneKey = (String)k2dit.next();
      Experiment psi = (Experiment)experiments_.get(pruneKey);
      retval.expSubsetOrig.put(pruneKey, psi.clone());
      psi.deleteInvestigator(investID);
      retval.expSubsetNew.put(pruneKey, psi.clone());
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  } 
 
  /***************************************************************************
  **
  ** Drop data point annotations.  The set of keys is a superset of
  ** those that are relevant!
  */
  
  public PertDataChange dropDataPointAnnotations(String annotID, Set keysToPrune) {
    PertDataChange retval = null;
    Iterator kpit = keysToPrune.iterator();
    while (kpit.hasNext()) {
      String dpkey = (String)kpit.next();
      ArrayList noteList = (ArrayList)dataPointNotes_.get(dpkey); 
      if (noteList != null) {
        if (retval == null) {
          retval = new PertDataChange(serialNumber_, PertDataChange.DATA_ANNOTS_SET);  
          retval.dataPtNotesSubsetOrig = new HashMap();
          retval.dataPtNotesSubsetNew = new HashMap();
        }
        retval.dataPtNotesSubsetOrig.put(dpkey, noteList.clone());
        noteList.remove(annotID);
        if (noteList.isEmpty()) {
          dataPointNotes_.remove(dpkey); 
        }
        // Empty list MUST cause deletion on redo
        retval.dataPtNotesSubsetNew.put(dpkey, noteList.clone()); 
      }
    }
    if (retval != null) {
      retval.serialNumberNew = ++serialNumber_;
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo a data point annotation set change
  */
  
  private void dataPointAnnotSetUndo(PertDataChange undo) {
    Iterator dpnkit = undo.dataPtNotesSubsetOrig.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = (String)dpnkit.next();
      ArrayList dpn = (ArrayList)undo.dataPtNotesSubsetOrig.get(key);
      ArrayList dpnc = (ArrayList)dpn.clone();
      dataPointNotes_.put(key, dpnc);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo data point annotation change
  */
  
  private void dataPointAnnotSetRedo(PertDataChange redo) {
    Iterator dpnkit = redo.dataPtNotesSubsetNew.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = (String)dpnkit.next();
      ArrayList dpn = (ArrayList)redo.dataPtNotesSubsetNew.get(key);
      ArrayList dpnc = (ArrayList)dpn.clone();
      if (dpnc.isEmpty()) {  // Empty list means delete
        dataPointNotes_.remove(key);
      } else {
        dataPointNotes_.put(key, dpnc);
      }
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }
  
  /***************************************************************************
  **
  ** Drop data point controls.
  */
  
  public PertDataChange dropDataPointControls(String annotID, Set keysToPrune) {
    PertDataChange retval = null;
    Iterator kpit = keysToPrune.iterator();
    while (kpit.hasNext()) {
      String dpkey = (String)kpit.next();
      PertDataPoint pdp = (PertDataPoint)dataPoints_.get(dpkey); 
      if (retval == null) {
        retval = new PertDataChange(serialNumber_, PertDataChange.DATA_POINT_SET);  
        retval.dataPtsSubsetOrig = new HashMap();
        retval.dataPtsSubsetNew = new HashMap();
      }
      retval.dataPtsSubsetOrig.put(dpkey, pdp.clone());
      pdp.setControl(null);
      retval.dataPtsSubsetNew.put(dpkey, pdp.clone());
    }
    if (retval != null) {
      retval.serialNumberNew = ++serialNumber_;
    }
    return (retval);
  } 

  /***************************************************************************
  **
  ** Drop annotation from source definitions
  */
  
  public PertDataChange dropSourceDefAnnotations(String annotID, Set keysToPrune) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.SOURCE_DEF_SET);
    
    retval.srcDefsSubsetOrig = new HashMap();
    Iterator psdit = keysToPrune.iterator();
    while (psdit.hasNext()) {
      String sdkey = (String)psdit.next();
      PertSource ps = (PertSource)sourceDefs_.get(sdkey);
      retval.srcDefsSubsetOrig.put(sdkey, ps.clone());
    }
    
    Iterator k2dit = keysToPrune.iterator();
    while (k2dit.hasNext()) {
      String pruneKey = (String)k2dit.next();
      PertSource ps = (PertSource)sourceDefs_.get(pruneKey);
      List psan = new ArrayList(ps.getAnnotationIDs());
      psan.remove(annotID);    
      ps.setAnnotationIDs(psan);
    }
    
    retval.srcDefsSubsetNew = new HashMap();
    psdit = keysToPrune.iterator();
    while (psdit.hasNext()) {
      String sdkey = (String)psdit.next();
      PertSource ps = (PertSource)sourceDefs_.get(sdkey);
      retval.srcDefsSubsetNew.put(sdkey, ps.clone());
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Undo a pert source set change
  */
  
  private void sourceDefSetUndo(PertDataChange undo) {
    Iterator dpnkit = undo.srcDefsSubsetOrig.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = (String)dpnkit.next();
      PertSource ps = (PertSource)undo.srcDefsSubsetOrig.get(key);
      PertSource psc = (PertSource)ps.clone();
      sourceDefs_.put(key, psc);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo pert source set change
  */
  
  private void sourceDefSetRedo(PertDataChange redo) {
    Iterator dpnkit = redo.srcDefsSubsetNew.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = (String)dpnkit.next();
      PertSource ps = (PertSource)redo.srcDefsSubsetNew.get(key);
      PertSource psc = (PertSource)ps.clone();
      sourceDefs_.put(key, psc);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }

  /***************************************************************************
  **
  ** Drop target annotations.  The set of keys is a superset of
  ** those that are relevant!
  */
  
  public PertDataChange dropTargetAnnotations(String annotID, Set keysToPrune) {
    PertDataChange retval = null;
    Iterator kpit = keysToPrune.iterator();
    while (kpit.hasNext()) {
      String dpkey = (String)kpit.next();
      ArrayList noteList = (ArrayList)targetGeneNotes_.get(dpkey); 
      if (noteList != null) {
        if (retval == null) {
          retval = new PertDataChange(serialNumber_, PertDataChange.TARG_ANNOTS_SET);  
          retval.targetNotesSubsetOrig = new HashMap();
          retval.targetNotesSubsetNew = new HashMap();
        }
        retval.targetNotesSubsetOrig.put(dpkey, noteList.clone());
        noteList.remove(annotID);
        if (noteList.isEmpty()) {
          targetGeneNotes_.remove(dpkey); 
        }
        // Empty list MUST cause deletion on redo!!!!      
        retval.targetNotesSubsetNew.put(dpkey, noteList.clone());
      }
    }
    if (retval != null) {
      retval.serialNumberNew = ++serialNumber_;
    }
    return (retval);
  } 

  /***************************************************************************
  **
  ** Drop a bunch of Pert source definitions
  */
  
  public PertDataChange[] deletePertSourceDefs(Set keysToDrop) {
    ArrayList retlist = new ArrayList();
    Iterator kit = keysToDrop.iterator();
    while (kit.hasNext()) {
      String keyID = (String)kit.next();
      retlist.add(deleteSourceDef(keyID));
    }
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge all source names to a single one
  */
  
  public PertDataChange mergePertSourceNameRefs(Set defsToMerge, String srcKey, Set abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.SOURCE_DEF_SET);
    retval.srcDefsSubsetOrig = new HashMap();
    retval.srcDefsSubsetNew = new HashMap();
    Iterator k2dit = defsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = (String)k2dit.next();
      PertSource psd = (PertSource)sourceDefs_.get(key);
      boolean replaceProx = false;
      boolean replaceSrc = false;
      if (psd.isAProxy()) {
        String proxKey = psd.getProxiedSpeciesKey();
        replaceProx = abandonKeys.contains(proxKey) && !proxKey.equals(srcKey);       
      }
      String srcNameKey = psd.getSourceNameKey();
      replaceSrc = abandonKeys.contains(srcNameKey) && !srcNameKey.equals(srcKey);
      if (replaceProx || replaceSrc) {
        retval.srcDefsSubsetOrig.put(key, psd.clone());
        if (replaceProx) {
          psd.setProxiedSpeciesKey(srcKey);
        }
        if (replaceSrc) {
          psd.setSourceNameKey(srcKey);
        }
        retval.srcDefsSubsetNew.put(key, psd.clone()); 
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }

  /***************************************************************************
  **
  ** Merge pert properties refs
  */
  
  public PertDataChange mergePertPropRefs(Set defsToMerge, String typeKey) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.SOURCE_DEF_SET);
    retval.srcDefsSubsetOrig = new HashMap();
    retval.srcDefsSubsetNew = new HashMap();
    Iterator k2dit = defsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = (String)k2dit.next();
      PertSource psd = (PertSource)sourceDefs_.get(key);
      if (!psd.getExpTypeKey().equals(typeKey)) {
        retval.srcDefsSubsetOrig.put(key, psd.clone());
        psd.setExpType(typeKey);
        retval.srcDefsSubsetNew.put(key, psd.clone());
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }

  /***************************************************************************
  **
  ** Return the serialNumbers
  */
  
  public SerialNumberSet getSerialNumbers() {
    return (new SerialNumberSet(serialNumber_, entryMap_.getSerialNumber(), sourceMap_.getSerialNumber()));
  }
  
  /***************************************************************************
  **
  ** Set the serialNumber.  Only used in db undo transaction closings!
  */
  
  public void setSerialNumber(long serialNumber) {
    serialNumber_ = serialNumber;
    return;
  }

  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void changeUndo(PertDataChange undo) {
    switch (undo.mode) {
      case PertDataChange.DATA_POINT:
        dataPointUndo(undo);
        return;
      case PertDataChange.EXPERIMENT:
        experimentUndo(undo);
        return;
      case PertDataChange.DATA_POINT_SET:
        dataPointSetUndo(undo);
        return;  
      case PertDataChange.EXPERIMENT_SET:
        experimentSetUndo(undo);
        return;   
      case PertDataChange.SOURCE_DEF:
        sourceDefUndo(undo);
        return;  
      case PertDataChange.USER_FIELD_VALS:
        userFieldValsUndo(undo);
        return;  
      case PertDataChange.USER_FIELD_NAME:
        userFieldNameUndo(undo);
        return; 
      case PertDataChange.REG_RESTRICT:
        regRestrictUndo(undo);
        return;
      case PertDataChange.DATA_ANNOTS:
        dataAnnotUndo(undo);
        return;
      case PertDataChange.TARGET_ANNOTS:
        targetAnnotUndo(undo);
        return;       
      case PertDataChange.SOURCE_NAME:
        sourceNameUndo(undo);
        return;
      case PertDataChange.EXP_CONTROL:
        expControlUndo(undo);
        return;
      case PertDataChange.INVEST:
        investUndo(undo);
        return;
      case PertDataChange.DATA_ANNOTS_SET:
        dataPointAnnotSetUndo(undo);
        return;
      case PertDataChange.SOURCE_DEF_SET:  
        sourceDefSetUndo(undo);
        return;
      case PertDataChange.MEASURE_PROP:
        measurePropUndo(undo);
        return;
      case PertDataChange.EXP_COND:
        expCondUndo(undo);
        return;
      case PertDataChange.MEAS_SCALE:
        measureScaleUndo(undo);
        return;
      case PertDataChange.TARGET_NAME:
        targetNameUndo(undo);
        return;
      case PertDataChange.ANNOTATION:
        annotationUndo(undo);
        return;
      case PertDataChange.PERT_PROP:
        pertPropUndo(undo);
        return;
      case PertDataChange.TARG_ANNOTS_SET:
        targetAnnotSetUndo(undo);
        return;
      case PertDataChange.ANNOT_MODULE:  
        annotModuleUndo(undo); 
        return;
      case PertDataChange.NAME_MAPPER:
        if (undo.nameMapperMode.equals("targets")) {
          entryMap_.changeUndo(undo);
        } else if (undo.nameMapperMode.equals("sources")) {
          sourceMap_.changeUndo(undo);
        } else {
          throw new IllegalArgumentException();
        }
        return;  
      case PertDataChange.MEASURE_PROP_SET:  
        measurePropSetUndo(undo); 
        return; 
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void changeRedo(PertDataChange redo) {
    switch (redo.mode) {
      case PertDataChange.DATA_POINT:
        dataPointRedo(redo);
        return;
      case PertDataChange.EXPERIMENT:
        experimentRedo(redo);
        return;
      case PertDataChange.DATA_POINT_SET:
        dataPointSetRedo(redo);
        return;
      case PertDataChange.EXPERIMENT_SET:
        experimentSetRedo(redo);
        return;
      case PertDataChange.SOURCE_DEF:
        sourceDefRedo(redo);
        return;
      case PertDataChange.USER_FIELD_VALS:
        userFieldValsRedo(redo);
        return;  
      case PertDataChange.USER_FIELD_NAME:
        userFieldNameRedo(redo);
        return;
      case PertDataChange.REG_RESTRICT:
        regRestrictRedo(redo);
        return;
      case PertDataChange.DATA_ANNOTS:
        dataAnnotRedo(redo);
        return;
      case PertDataChange.TARGET_ANNOTS:
        targetAnnotRedo(redo);
        return;
      case PertDataChange.SOURCE_NAME:
        sourceNameRedo(redo);
        return;
      case PertDataChange.EXP_CONTROL:
        expControlRedo(redo);
        return;
      case PertDataChange.INVEST:
        investRedo(redo);
        return;
      case PertDataChange.DATA_ANNOTS_SET:
        dataPointAnnotSetRedo(redo);
        return;
      case PertDataChange.SOURCE_DEF_SET:  
        sourceDefSetRedo(redo);
        return;
      case PertDataChange.MEASURE_PROP:
        measurePropRedo(redo);
        return;
      case PertDataChange.EXP_COND:
        expCondRedo(redo);
        return;
      case PertDataChange.MEAS_SCALE:
        measureScaleRedo(redo);
        return;
      case PertDataChange.TARGET_NAME:
        targetNameRedo(redo);
        return;
      case PertDataChange.ANNOTATION:
        annotationRedo(redo);
        return;
      case PertDataChange.PERT_PROP:
        pertPropRedo(redo);
        return;
      case PertDataChange.TARG_ANNOTS_SET:
        targetAnnotSetRedo(redo);
        return;
      case PertDataChange.ANNOT_MODULE:  
        annotModuleRedo(redo);   
        return;
      case PertDataChange.NAME_MAPPER:
        if (redo.nameMapperMode.equals("targets")) {
          entryMap_.changeRedo(redo);
        } else if (redo.nameMapperMode.equals("sources")) {
          sourceMap_.changeRedo(redo);
        } else {
          throw new IllegalArgumentException();
        }
        return;
      case PertDataChange.MEASURE_PROP_SET:  
        measurePropSetRedo(redo); 
        return; 
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** Answers if we have perturbation sources available
  **
  */
  
  public boolean havePertSources() {      
    return (!sourceDefs_.isEmpty());
  }
  
  /***************************************************************************
  **
  ** Answers if we have perturbation data
  **
  */
  
  public boolean haveData() {
 
    if (dataPoints_.size() > 0) {
      return (true);
    }
    if (!experiments_.isEmpty()) {
      return (true);
    }
    
    if (pertDict_.getPerturbPropsCount() > PertDictionary.LEGACY_PERTURB_TYPES) {
      return (true);
    }
          
    if (measureDict_.haveData()) {
      return (true);
    }
    
    if (condDict_.haveData()) {
      return (true);
    }
  
    if (pertAnnot_.haveMessages()) {
      return (true);
    }
    
    if (!targets_.isEmpty()) {
      return (true);
    }
    
    if (!sourceNames_.isEmpty()) {
      return (true);
    }
  
    if (!investigators_.isEmpty()) {
      return (true);
    }
    
    if (!sourceDefs_.isEmpty()) {
      return (true);
    }
 
    if (!regionRestrictions_.isEmpty()) {
      return (true);
    }
      
    if (entryMap_.haveData()) {
      return (true);
    }  
    
    if (sourceMap_.haveData()) {
      return (true);
    }    
    
    return (false);
  }
  
  /***************************************************************************
  **
  ** Gets time range for perturbation data
  **
  */
  
  public MinMax getDataRange() {
    int numSrc = experiments_.size();
    if (numSrc == 0) {
      return (null);
    }
    
    MinMax retval = (new MinMax()).init();
  
    Iterator psit = experiments_.values().iterator();
    while (psit.hasNext()) {
      Experiment prs = (Experiment)psit.next();
      int time = prs.getTime();
      int legMax = prs.getLegacyMaxTime();
      if (time != Experiment.NO_TIME) {
        retval.update(time);
      }
      if (legMax != Experiment.NO_TIME) {
        retval.update(legMax);
      }
    }
  
    boolean haveVals = (retval.min != Integer.MAX_VALUE) && (retval.max != Integer.MIN_VALUE);                
    return (haveVals ? retval : null);    
  }  

  /***************************************************************************
  **
  ** Answers if we have a name mapping
  **
  */
  
  public boolean haveCustomEntryMapForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    return (entryMap_.haveCustomMapForNode(nodeID));
  }
  
  /***************************************************************************
  **
  ** Answers if we have a name mapping
  **
  */
  
  public boolean haveCustomSourceMapForNode(String nodeID) {
    if (!haveData()) {
      return (false);
    }
    return (sourceMap_.haveCustomMapForNode(nodeID));
  }
  
  /***************************************************************************
  **
  ** Answers if we have a name mapping
  **
  */
  
  public boolean haveCustomMapForNode(String nodeID) {
    return (haveCustomSourceMapForNode(nodeID) || haveCustomEntryMapForNode(nodeID));
  }

  /***************************************************************************
  **
  ** Answers if we have perturbation data for the given node.  If sourceID is
  ** not null, we look for data for the source into the targ
  **
  */
  
  public boolean haveDataForNode(String nodeID, String sourceID) {
    if (!haveData()) {
      return (false);
    }
    List mapped = getDataEntryKeysWithDefault(nodeID);
    if ((mapped == null) || mapped.isEmpty()) {
      return (false);
    }
    
    List smapped = null;
    if (sourceID != null) {
      smapped = getDataSourceKeysWithDefault(sourceID);
      if ((smapped == null) || smapped.isEmpty()) {
        return (false);
      }  
    }
     
    Iterator mit = mapped.iterator();
    while (mit.hasNext()) {
      String key = (String)mit.next();
      
      if (smapped == null) {
        PertFilterExpression pfe = new PertFilterExpression(new PertFilter(PertFilter.TARGET, PertFilter.STR_EQUALS, key));
        List pertsWithTarg = getPerturbations(pfe);
        if (!pertsWithTarg.isEmpty()) {
          return (true);
        }
      } else {
        PertFilter tfilt = new PertFilter(PertFilter.TARGET, PertFilter.STR_EQUALS, key);
        Iterator smit = smapped.iterator();
        while (smit.hasNext()) {
          String skey = (String)smit.next();
          PertFilter sfilt = new PertFilter(PertFilter.SOURCE_NAME, PertFilter.STR_EQUALS, skey);  
          PertFilterExpression pfe = new PertFilterExpression(PertFilterExpression.AND_OP, tfilt, sfilt);
          List pertsWithSrcAndTarg = getPerturbations(pfe);
          if (!pertsWithSrcAndTarg.isEmpty()) {
            return (true);
          }
        }
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** We are changing the name of a node or gene, and have been told to modify the
  ** data as needed.  We need to change the source name and target name tables.
  ** We should not get any identity maps generated, but just in case...
  */
  
  
  public PertDataChange[] changeName(String oldName, String newName) {
    ArrayList retlist = new ArrayList();
    
    String foundTarg = getTargKeyFromName(oldName);
    if (foundTarg != null) {
      retlist.addAll(Arrays.asList(changeTargetNameManageIdents(foundTarg, newName)));
    }
    
    String foundSrc = getSourceKeyFromName(oldName);
    if (foundSrc != null) {
      retlist.addAll(Arrays.asList(setSourceName(foundSrc, newName)));
    }

    PertDataChange[] changes = (PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]);
    return (changes);
  }  
  
  /***************************************************************************
  **
  ** Add or replace an experiment
  */
  
  public PertDataChange setExperiment(Experiment exp) { 
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXPERIMENT); 
    Experiment currExp = (Experiment)experiments_.get(exp.getID());
    retval.expOrig = (currExp == null) ? null : (Experiment)currExp.clone();
    experiments_.put(exp.getID(), exp);
    retval.expNew = (Experiment)exp.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }

  /***************************************************************************
  **
  ** Find batch IDs for an experiment
  */
  
  public Map getBatchKeysForExperiment(String expKey) {
    HashMap retval = new HashMap();
    Iterator dpit = dataPoints_.keySet().iterator();
    while (dpit.hasNext()) {
      String dpKey = (String)dpit.next();
      PertDataPoint pdp = (PertDataPoint)dataPoints_.get(dpKey);
      if (expKey.equals(pdp.getExperimentKey())) {
        String batchID = pdp.getBatchKey();
        String targetID = pdp.getTargetKey();
        HashSet forTarg = (HashSet)retval.get(targetID);
        if (forTarg == null) {
          forTarg = new HashSet();
          retval.put(targetID, forTarg);
        }     
        forTarg.add(batchID);
      }
    }   
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Find batch ID collisions for merging experiments
  */
  
  public Map mergeExperimentBatchCollisions(Set keyIDs) {
    HashMap seenKeys = new HashMap();
    Iterator dpit = dataPoints_.keySet().iterator();
    while (dpit.hasNext()) {
      String dpKey = (String)dpit.next();
      PertDataPoint pdp = (PertDataPoint)dataPoints_.get(dpKey);
      String expKey = pdp.getExperimentKey();
      if (keyIDs.contains(expKey)) {
        String batchID = pdp.getBatchKey();
        String targetID = pdp.getTargetKey();    
        HashMap forTarg = (HashMap)seenKeys.get(targetID);
        if (forTarg == null) {
          forTarg = new HashMap();
          seenKeys.put(targetID, forTarg);
        }
        ArrayList vals = (ArrayList)forTarg.get(batchID);
        if (vals == null) {
          vals = new ArrayList();
          forTarg.put(batchID, vals);
        }
        vals.add(pdp.getDisplayValue());
      }
    }
    return (seenKeys);
  }
  
  /***************************************************************************
  **
  ** Find batch ID collisions for merging experiments
  */

  public Map prepBatchCollisions(Map collisions, String expString) {   
    boolean haveOne = false;
    TreeMap retval = new TreeMap();
    TreeMap perExp = new TreeMap();
    retval.put(expString, perExp);
    Iterator cit = collisions.keySet().iterator();
    while (cit.hasNext()) {
      String targetID = (String)cit.next();
      Map forTarg = (Map)collisions.get(targetID);
      TreeMap perTarg = new TreeMap();
      String targName = getTarget(targetID);
      perExp.put(targName, perTarg);
      Iterator ftit = forTarg.keySet().iterator();
      while (ftit.hasNext()) {
        String batchID = (String)ftit.next();    
        ArrayList vals = (ArrayList)forTarg.get(batchID);
        int numVal = vals.size();
        if (numVal > 1) {
          BatchCollision bc = new BatchCollision(expString, targName, batchID);
          perTarg.put(batchID, bc);
          bc.vals.addAll(vals);
          haveOne = true;
        }
      }  
    }
    return (haveOne ? retval : null);
  }
  

  /***************************************************************************
  **
  ** Merge experiments
  */
  
  public PertDataChange[] mergeExperiments(List keyIDs, String useKey, Experiment revisedExp) { 
    ArrayList retlist = new ArrayList();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = (String)keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setExperiment(revisedExp));
      } else {
        retlist.add(deleteExperiment(keyID));
      }
    }
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge all experiment refs to a single one
  */
  
  public PertDataChange mergeExperimentRefs(Set pointsToMerge, String expKey, Set abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.DATA_POINT_SET);
    retval.dataPtsSubsetOrig = new HashMap();
    retval.dataPtsSubsetNew = new HashMap();
    Iterator k2dit = pointsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = (String)k2dit.next();
      PertDataPoint pdp = (PertDataPoint)dataPoints_.get(key);
      if (!pdp.getExperimentKey().equals(expKey)) {
        retval.dataPtsSubsetOrig.put(key, pdp.clone());
        pdp.setExperimentKey(expKey);
        retval.dataPtsSubsetNew.put(key, pdp.clone());
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Delete an experiment
  */
  
  public PertDataChange deleteExperiment(String psiID) { 
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.EXPERIMENT);    
    retval.expOrig = (Experiment)((Experiment)experiments_.get(psiID)).clone();
    experiments_.remove(psiID);
    labels_.removeLabel(psiID);
    retval.expNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo an experiment change
  */
  
  private void experimentUndo(PertDataChange undo) {    
    if (undo.expOrig == null) {  // Undo an add
      String repairID = undo.expNew.getID();
      experiments_.remove(repairID);
      labels_.removeLabel(repairID);
    } else if (undo.expNew == null) {  // Undo a delete
      Experiment expu = (Experiment)undo.expOrig.clone();
      String repairID = expu.getID();
      experiments_.put(repairID, expu);
      labels_.addExistingLabel(repairID);
    } else {  // Change data
      Experiment expu = (Experiment)undo.expOrig.clone();
      String repairID = expu.getID();
      experiments_.put(repairID, expu);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo an experiment change
  */
  
  private void experimentRedo(PertDataChange redo) {
    if (redo.expOrig == null) {  // Redo an add
      Experiment expn = (Experiment)redo.expNew.clone();
      String repairID = expn.getID();
      experiments_.put(repairID, expn);
      labels_.addExistingLabel(repairID);
    } else if (redo.expNew == null) {  // Redo a delete
      String repairID = redo.expOrig.getID();
      experiments_.remove(repairID);
      labels_.removeLabel(repairID);
    } else {  // Change data
      Experiment expn = (Experiment)redo.expNew.clone();
      String repairID = expn.getID();
      experiments_.put(repairID, expn);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }
  
  /***************************************************************************
  **
  ** Undo an experiment set change
  */
  
  private void experimentSetUndo(PertDataChange undo) {
    if (undo.expSubsetOrig == null) {
      throw new IllegalStateException();
    } else if (undo.expSubsetNew == null) {  // Undo a delete
      Iterator kit = undo.expSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = (String)kit.next();
        Experiment exp = (Experiment)undo.expSubsetOrig.get(key);
        Experiment expc = (Experiment)exp.clone();
        experiments_.put(key, expc);
        labels_.addExistingLabel(key);
      }
    } else {  // Change data
      Iterator kit = undo.expSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = (String)kit.next();
        Experiment exp = (Experiment)undo.expSubsetOrig.get(key);
        Experiment expc = (Experiment)exp.clone();
        experiments_.put(key, expc);
      }
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo an experiment set change
  */
  
  private void experimentSetRedo(PertDataChange redo) {
     if (redo.expSubsetOrig == null) {
      throw new IllegalStateException();
    } else if (redo.expSubsetNew == null) {  // redo a delete
      Iterator kit = redo.expSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = (String)kit.next();
        experiments_.remove(key);
        labels_.removeLabel(key);
      }
    } else {  // Redo Change data
      Iterator kit = redo.expSubsetNew.keySet().iterator();
      while (kit.hasNext()) {
        String key = (String)kit.next();
        Experiment exp = (Experiment)redo.expSubsetNew.get(key);
        Experiment expc = (Experiment)exp.clone();
        experiments_.put(key, expc);
      }
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 
 
  /***************************************************************************
  **
  ** Get the Experiment for the given key:
  */
  
  public Experiment getExperiment(String key) {
    return ((Experiment)experiments_.get(key));
  }
  
  /***************************************************************************
  **
  ** Get the keys for the experiments:
  */
  
  public Iterator<String> getExperimentKeys() {
    return (new TreeSet<String>(experiments_.keySet()).iterator());
  }   
  
  /***************************************************************************
  **
  ** Set a perturbation source def
  */
  
  public PertDataChange setSourceDef(PertSource ps) { 
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.SOURCE_DEF);    
    PertSource currPs = (PertSource)sourceDefs_.get(ps.getID());
    retval.srcDefOrig = (currPs == null) ? null : (PertSource)currPs.clone();
    sourceDefs_.put(ps.getID(), ps);
    retval.srcDefNew = (PertSource)ps.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete perturbation source def
  */
  
  public PertDataChange deleteSourceDef(String psID) { 
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.SOURCE_DEF);    
    retval.srcDefOrig = (PertSource)((PertSource)sourceDefs_.get(psID)).clone();
    sourceDefs_.remove(psID);
    retval.srcDefNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Merge perturbation source defs
  */
  
  public PertDataChange[] mergePertSourceDefs(List keyIDs, String useKey, PertSource revisedPsrc) { 
    ArrayList retlist = new ArrayList();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = (String)keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setSourceDef(revisedPsrc));
      } else {
        retlist.add(deleteSourceDef(keyID));
      }
    }
    return ((PertDataChange[])retlist.toArray(new PertDataChange[retlist.size()]));
  }

  /***************************************************************************
  **
  ** Undo a source def change
  */
  
  private void sourceDefUndo(PertDataChange undo) {
    if (undo.srcDefOrig == null) { // Undo an add
      String repairID = undo.srcDefNew.getID();
      sourceDefs_.remove(repairID);
      labels_.removeLabel(repairID);
    } else if (undo.srcDefNew == null) {  // Undo a delete
      String repairID = undo.srcDefOrig.getID();
      PertSource psc = (PertSource)undo.srcDefOrig.clone();
      sourceDefs_.put(repairID, psc);
      labels_.addExistingLabel(repairID);
    } else {  // Change data
      String repairID = undo.srcDefOrig.getID();
      PertSource psc = (PertSource)undo.srcDefOrig.clone();
      sourceDefs_.put(repairID, psc);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo an source def change
  */
  
  private void sourceDefRedo(PertDataChange redo) {
    if (redo.srcDefOrig == null) { // Redo an add
      String repairID = redo.srcDefNew.getID();
      PertSource psc = (PertSource)redo.srcDefNew.clone();
      sourceDefs_.put(repairID, psc);
      labels_.addExistingLabel(repairID);
    } else if (redo.srcDefNew == null) {  // Redo a delete
      String repairID = redo.srcDefOrig.getID();
      sourceDefs_.remove(repairID);
      labels_.removeLabel(repairID);
    } else {  // Change data
      String repairID = redo.srcDefNew.getID();
      PertSource psc = (PertSource)redo.srcDefNew.clone();
      sourceDefs_.put(repairID, psc);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 

  /***************************************************************************
  **
  ** Set a data point.  Id must have been registered already
  */
  
  public PertDataChange setDataPoint(PertDataPoint pdp) { 
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.DATA_POINT);    
    PertDataPoint currPdp = (PertDataPoint)dataPoints_.get(pdp.getID());
    retval.pdpOrig = (currPdp == null) ? null : (PertDataPoint)currPdp.clone();
    dataPoints_.put(pdp.getID(), pdp);
    retval.pdpNew = (PertDataPoint)pdp.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Undo a data point change
  */
  
  private void dataPointUndo(PertDataChange undo) {    
    if (undo.pdpOrig == null) {  // Undo an add
      String repairID = undo.pdpNew.getID();
      dataPoints_.remove(repairID);
      labels_.removeLabel(repairID);
    } else if (undo.pdpNew == null) {  // Undo a delete
      PertDataPoint pdpu = undo.pdpOrig.clone();
      String repairID = pdpu.getID();
      dataPoints_.put(repairID, pdpu);
      labels_.addExistingLabel(repairID);
      if (undo.dataAnnotsOrig != null) {
        ArrayList notes = (ArrayList)undo.dataAnnotsOrig.clone();
        dataPointNotes_.put(repairID, notes);
      }
      if (undo.dataRegResOrig != null) {
        RegionRestrict regReg = undo.dataRegResOrig.clone();
        regionRestrictions_.put(repairID, regReg);
      }
      if (undo.userDataOrig != null) {
        ArrayList values = new ArrayList<String>(undo.userDataOrig);
        userData_.put(repairID, values);
      }
    } else {  // Change data
      PertDataPoint pdpu = undo.pdpOrig.clone();
      String repairID = pdpu.getID();
      dataPoints_.put(repairID, pdpu);
    }
    serialNumber_ = undo.serialNumberOrig;
    return;
  }
  
  /***************************************************************************
  **
  ** Redo a data point change
  */
  
  private void dataPointRedo(PertDataChange redo) {
    if (redo.pdpOrig == null) {  // Redo an add
      PertDataPoint pdpn = (PertDataPoint)redo.pdpNew.clone();
      String repairID = pdpn.getID();
      dataPoints_.put(repairID, pdpn);
      labels_.addExistingLabel(repairID);
    } else if (redo.pdpNew == null) {  // Redo a delete
      String repairID = redo.pdpOrig.getID();
      dataPoints_.remove(repairID);
      labels_.removeLabel(repairID); 
      if (redo.dataAnnotsOrig != null) {
        dataPointNotes_.remove(repairID);
      }
      if (redo.dataRegResOrig != null) {
        regionRestrictions_.remove(repairID);
      }
      if (redo.userDataOrig != null) {
        userData_.remove(repairID);
      }
    } else {  // Change data
      PertDataPoint pdpn = (PertDataPoint)redo.pdpNew.clone();
      String repairID = pdpn.getID();
      dataPoints_.put(repairID, pdpn);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }

  /***************************************************************************
  **
  ** Delete a data point
  */
  
  public PertDataChange deleteDataPoint(String pdpID) { 
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.DATA_POINT);    
    retval.pdpOrig = (PertDataPoint)((PertDataPoint)dataPoints_.get(pdpID)).clone();
    dataPoints_.remove(pdpID);
    labels_.removeLabel(pdpID);
    
    //
    // Remove annotations, if present:
    //
    
    ArrayList notes = (ArrayList)dataPointNotes_.get(pdpID);  // may be null
    if (notes != null) {
      retval.dataAnnotsOrig = (ArrayList)notes.clone();
      dataPointNotes_.remove(pdpID);
    }
    
    //
    // Remove region restrict, if present:
    //
    
    RegionRestrict regReg = (RegionRestrict)regionRestrictions_.get(pdpID);  // may be null
    if (regReg != null) {
      retval.dataRegResOrig = (RegionRestrict)regReg.clone();
      regionRestrictions_.remove(pdpID);
    } 
    
    //
    // Remove user data:
    //
    
    ArrayList values = (ArrayList)userData_.get(pdpID);  // may be null
    if (values != null) {
      retval.userDataOrig = (ArrayList)values.clone();
      userData_.remove(pdpID);
    }

    retval.pdpNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drop the maps to an entry that is about to disappear
  */
  
  public PertDataChange[] dropDanglingMapsForEntry(String entryID) {
    return (entryMap_.dropDanglingMapsFor(entryID));
  }  
  
  /***************************************************************************
  **
  ** Drop the perturbation data matching the filter
  */
   
  public PertDataChange dropPertDataForFilter(PertFilterExpression pfe) {
    List dropPoints = getPerturbations(pfe);
    HashSet keysToDrop = new HashSet();
    int numNew = dropPoints.size();
    for (int i = 0; i < numNew; i++) {
      PertDataPoint pdp = (PertDataPoint)dropPoints.get(i);
      keysToDrop.add(pdp.getID());
    }   
    return (deleteDataPoints(keysToDrop));
  }
     
  /***************************************************************************
  **
  ** Gets network elements for a root network
  */
  
  public Set getNetworkElements() {
    HashSet retval = new HashSet();
    List sigPerts = getSignificantPerturbations();
    int numSig = sigPerts.size();
    for (int i = 0; i < numSig; i++) {
      PertDataPoint pdp = (PertDataPoint)sigPerts.get(i);
      String tname = pdp.getTargetName(this);
      retval.add(new QpcrSourceSet(tname, 0));
      Experiment psi = pdp.getExperiment(this);
      if (psi.isSinglePerturbation()) {
        PertSource ps = psi.getSources().getSinglePert(this);
        retval.add(new QpcrSourceSet(ps.getSourceName(this), 0));
      } else {
        retval.add(new QpcrSourceSet(psi.getPertDisplayString(this, PertSources.NO_FOOTS), psi.getSources().getNumSources()));
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Use for IO
  */
  
  public void installNameMap(NameMapper nMap) {
    String usage = nMap.getUsage();
    if (usage.equals("targets")) {
      entryMap_ = nMap;
    } else if (usage.equals("sources")) {
      sourceMap_ = nMap;
    } else {
      throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Set the map from a node to a List of target nodes
  */
  
  public PertDataChange setEntryMap(String key, List entries) {
    return (entryMap_.setDataMap(key, entries));
  }
  
  /***************************************************************************
  **
  ** Set the map from a node to a List of targets, used in legacy
  ** imports only!
  */
  
  public void importLegacyEntryMapEntry(String key, List entries) {
    buildInvertTrgNameCache();
    entryMap_.importLegacyMapEntry(key, entries, invertTargNameCache_);
    return;
  }
  
  /***************************************************************************
  **
  ** Set the map from a node to a List of sources, used in legacy
  ** imports only!
  */
  
  public void importLegacySourceMapEntry(String key, List entries) {
    buildInvertSrcNameCache();
    sourceMap_.importLegacyMapEntry(key, entries, invertSrcNameCache_);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a map from a gene to a List of source genes
  */
  
  public PertDataChange setSourceMap(String key, List entries) {
    return (sourceMap_.setDataMap(key, entries));
  }
  
  /***************************************************************************
  **
  ** Add both entry and source maps
  */
  
  public PertDataChange[] addDataMaps(String key, List entries, List sources) { 
    ArrayList retvalList = new ArrayList();
    PertDataChange retval;
    if ((entries != null) && (entries.size() > 0)) {
      retval = setEntryMap(key, entries);
      retvalList.add(retval);
    }
    if ((sources != null) && (sources.size() > 0)) {
      retval = setSourceMap(key, sources);
      retvalList.add(retval);
    }
    return (PertDataChange[])retvalList.toArray(new PertDataChange[retvalList.size()]);
  }
  
  /***************************************************************************
  **
  ** Get the list of targets names for the gene ID.  May be empty.
  */
  
  public List<String> getDataEntryKeysWithDefault(String nodeId) {
    String name = ((DBGenome)appState_.getDB().getGenome()).getNode(nodeId).getName();
    return (entryMap_.getDataKeysWithDefault(nodeId, getTargKeyFromName(name)));
  }
  
  /***************************************************************************
  **
  ** Get the list of source names for the gene ID.  May be empty.
  */
  
  public List<String> getDataSourceKeysWithDefault(String nodeId) {
    String name = ((DBGenome)appState_.getDB().getGenome()).getNode(nodeId).getName();
    return (sourceMap_.getDataKeysWithDefault(nodeId, getSourceKeyFromName(name)));
  }

  /***************************************************************************
  **
  ** Get the list of targets names for the gene ID.  May be empty.
  */ 
  
  public List getDataEntryKeysWithDefaultGivenName(String nodeId, String nodeName) {  
    return (entryMap_.getDataKeysWithDefault(nodeId, getTargKeyFromName(nodeName)));
  }
  
  /***************************************************************************
  **
  ** Get the list of entry names for the gene ID.  May be null.
  */
  
  public List<String> getCustomDataEntryKeys(String geneId) {
    return (entryMap_.getCustomDataKeys(geneId));    
  }
  
  /***************************************************************************
  **
  ** Get the list of pert source keys for the gene ID.  May be null.
  */
  
  public List<String> getCustomDataSourceKeys(String geneId) {
    return (sourceMap_.getCustomDataKeys(geneId));    
  }  

  /***************************************************************************
  **
  ** Get the node IDs mapped to the given perturbation target key
  */

  public Set<String> getDataEntryKeyInverse(String key) {
    return (entryMap_.getDataKeyInverse(key, targets_));
  }
  
  /***************************************************************************
  **
  ** Get the node IDs mapped to the given perturbation source key
  */

  public Set<String> getDataSourceKeyInverse(String key) {
    return (sourceMap_.getDataKeyInverse(key, sourceNames_));
  }
  
  /***************************************************************************
  **
  ** Answer if we are vulnerable to a disconnect:
  */

  public boolean dataEntryOnlyInverseIsDefault(String key) {
    return (entryMap_.onlyInverseIsDefault(key, targets_));
  }
  
  /***************************************************************************
  **
  ** Answer if we are vulnerable to a disconnect:
  */

  public boolean dataSourceOnlyInverseIsDefault(String key) {
    return (sourceMap_.onlyInverseIsDefault(key, sourceNames_));
  }

  /***************************************************************************
  **
  ** Get the Set of keys that map to targets.
  */
  
  public Set getDataEntryKeySet() {
    return (entryMap_.getDataKeySet());
  }
  
  /***************************************************************************
  **
  ** Get the Set of keys that map to sources.
  */
  
  public Set getDataSourceKeySet() {
    return (sourceMap_.getDataKeySet());
  }  
 
  /***************************************************************************
  **
  ** Delete the key mapping
  */
  
  public PertDataChange dropDataEntryKeys(String geneId) {
    if (!entryMap_.haveCustomMapForNode(geneId) && !entryMap_.haveEmptyMapForNode(geneId)) {
      return (null);
    }
    return (entryMap_.dropDataKeys(geneId));
  }
  
  /***************************************************************************
  **
  ** Delete the key mapping
  */
  
  public PertDataChange dropDataSourceKeys(String geneId) {
    if (!sourceMap_.haveCustomMapForNode(geneId) && !sourceMap_.haveEmptyMapForNode(geneId)) {
      return (null);
    }
    return (sourceMap_.dropDataKeys(geneId));
  }
  
  /***************************************************************************
  **
  ** Drop all entry key mappings.  Big serial number bumping!
  */
  
  public PertDataChange[] dropAllDataEntryKeys() {   
    ArrayList retval = new ArrayList();
    Iterator ksit = new HashSet(getDataEntryKeySet()).iterator();
    while (ksit.hasNext()) {
      String id = (String)ksit.next(); 
      PertDataChange pdc = dropDataEntryKeys(id);
      retval.add(pdc);
    }  
    return ((PertDataChange[])retval.toArray(new PertDataChange[retval.size()]));    
  }
  
  
  
  /***************************************************************************
  **
  ** Drop all source key mappings.  Big serial number bumping!
  */
  
  public PertDataChange[] dropAllDataSourceKeys() {   
    ArrayList retval = new ArrayList();
    Iterator ksit = new HashSet(getDataSourceKeySet()).iterator();
    while (ksit.hasNext()) {
      String id = (String)ksit.next(); 
      PertDataChange pdc = dropDataSourceKeys(id);
      retval.add(pdc);
    }  
    return ((PertDataChange[])retval.toArray(new PertDataChange[retval.size()]));    
  }
    
  /***************************************************************************
  **
  ** Get single-source perturbation source IDs for the given targetID
  */
  
  public Set<String> getPerturbationSources(String targetID) {
    //
    // Get the genes that match the target ids.  Go through the
    // perturbation sources and find the perturbations.
    //
    HashSet<String> retval = new HashSet<String>();
  
    List stPerts = getSrcTargPerts(null, targetID);
    int numSt = stPerts.size();
    if (numSt == 0) {
      return (retval);
    }
      
    for (int i = 0; i < numSt; i++) {
      PertDataPoint pdp = (PertDataPoint)stPerts.get(i);
      String sskey = pdp.getSingleSourceKey(this);
      if (sskey != null) {
        retval.addAll(getDataSourceKeyInverse(sskey));
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the Ids of source names for proxies
  */
  
  public Set getProxiedSources() {
    HashSet retval = new HashSet();
    Iterator sdit = sourceDefs_.values().iterator();
    while (sdit.hasNext()) {
      PertSource chk = (PertSource)sdit.next();
      if (chk.isAProxy()) {
        retval.add(chk.getSourceNameKey());
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return map of proxy ID to proxied sources.
  */
  
  public Map getMappedProxiedSources() {
    HashMap retval = new HashMap();
    Iterator sdit = sourceDefs_.values().iterator();
    while (sdit.hasNext()) {
      PertSource chk = (PertSource)sdit.next();
      if (chk.isAProxy()) {
        String srcKey = chk.getSourceNameKey();
        HashSet allProx = (HashSet)retval.get(srcKey);
        if (allProx == null) {
          allProx = new HashSet();
          retval.put(srcKey, allProx);
        }     
        allProx.add(chk.getProxiedSpeciesKey());
      }
    }
    return (retval);
  }
   
  /***************************************************************************
  **
  ** Return the IDs of Pert sources that are proxies
  */
  
  public Set<String> getPertSourceKeysForProxies() {
    HashSet retval = new HashSet();
    Iterator sdit = sourceDefs_.keySet().iterator();
    while (sdit.hasNext()) {
      String chkID = (String)sdit.next();
      PertSource chk = sourceDefs_.get(chkID);
      if (chk.isAProxy()) {
        retval.add(chkID);
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the "significant" perturbations:
  */
  
  public List getSignificantPerturbations() {
    if ((sigPertCache_ == null) || (sigPertCacheVersionSN_ != serialNumber_)) {
      PertFilter pertFilter = new PertFilter(PertFilter.VALUE, PertFilter.ABOVE_THRESH, null);
      PertFilterExpression pfe = new PertFilterExpression(pertFilter);
      sigPertCache_ = getPerturbations(pfe);
      sigPertCacheVersionSN_ = serialNumber_;
    }
    return (sigPertCache_);
  }

  /***************************************************************************
  **
  ** Get a filter expression for the given src and target
  */
  
  public PertFilterExpression getFilterExpression(String sourceID, String targetID, boolean allowProxy) {
    List skeys = (sourceID == null) ? null : getDataSourceKeysWithDefault(sourceID);
    List tkeys = getDataEntryKeysWithDefault(targetID);
    return (getFilterExpression((sourceID == null), skeys, tkeys, allowProxy));
  }
  
  /***************************************************************************
  **
  ** Get a filter expression for the given src and target
  */
  
  public PertFilterExpression getFilterExpression(boolean skipSource, List skeys, List tkeys, boolean allowProxy) {
    int srcFilterCat = (allowProxy) ? PertFilter.SOURCE_OR_PROXY_NAME : PertFilter.SOURCE_NAME;
       
    PertFilterExpression srcExp = null;
    if ((skeys == null) || skeys.isEmpty()) {
      int useOp = (skipSource) ? PertFilterExpression.ALWAYS_OP : PertFilterExpression.NEVER_OP;
      srcExp = new PertFilterExpression(useOp);
      if (useOp == PertFilterExpression.NEVER_OP) {
        return (srcExp);
      }
    } else { 
      int numS = skeys.size();
      for (int i = 0; i < numS; i++) {
        String sKey = (String)skeys.get(i);
        PertFilter srcFilter = new PertFilter(srcFilterCat, PertFilter.STR_EQUALS, sKey);
        if (srcExp == null) {
          srcExp = new PertFilterExpression(srcFilter);
        } else {
          srcExp = new PertFilterExpression(PertFilterExpression.OR_OP, srcExp, srcFilter);
        }
      }
    }
    
    PertFilterExpression trgExp = null;
    if ((tkeys == null) || tkeys.isEmpty()) {
      trgExp = new PertFilterExpression(PertFilterExpression.NEVER_OP);
      return (trgExp);
    } else {    
      int numT = tkeys.size();
      for (int i = 0; i < numT; i++) {
        String tKey = (String)tkeys.get(i);
        PertFilter trgFilter = new PertFilter(PertFilter.TARGET, PertFilter.STR_EQUALS, tKey);
        if (trgExp == null) {
          trgExp = new PertFilterExpression(trgFilter);
        } else {
          trgExp = new PertFilterExpression(PertFilterExpression.OR_OP, trgExp, trgFilter);
        }
      }
    }
      
    PertFilterExpression exp = (srcExp == null) ? trgExp : new PertFilterExpression(PertFilterExpression.AND_OP, trgExp, srcExp);
    return (exp);
  }
  
  /***************************************************************************
  **
  ** Get the perturbations between the source and target
  */
  
  public List<PertDataPoint> getSrcTargPerts(String sourceID, String targetID) {
    PertFilterExpression fex = getFilterExpression(sourceID, targetID, false);
    return (getPerturbations(fex));
  } 
  
  /***************************************************************************
  **
  ** Answer if we have significant data matching the target, with relaxed name matching criteria:
  */
  
  public boolean haveDataRelaxedMatch(String targetName) {
    List sigPerts = getSignificantPerturbations();
    int numSig = sigPerts.size();
    for (int i = 0; i < numSig; i++) {
      PertDataPoint pdp = (PertDataPoint)sigPerts.get(i);
      String tname = pdp.getTargetName(this);
      if (DataUtil.keysEqual(tname, targetName)) {
        return (true);
      }
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Get the average perturbation value between the source and target
  */
  
  public double getPertAverageValue(String sourceID, String targetID, boolean avgSigOnly) { 
    List<PertDataPoint> stPerts = getSrcTargPerts(sourceID, targetID);
    int numSt = stPerts.size();
    if (numSt == 0) {
      return (0.0);
    }
    
    ArrayList<Double> doubleVals = new ArrayList<Double>();
    for (int i = 0; i < numSt; i++) {
      PertDataPoint pdp = stPerts.get(i);
      Double dValObj = pdp.getPerturbValue(this); 
      if (dValObj != null) {
        if (!avgSigOnly || pdp.isSignificant(this)) {
          doubleVals.add(dValObj);
        }
      }
    }
    return (DataUtil.avgVal(doubleVals));
  }

  /***************************************************************************
  **
  ** Get the tooltip for the given source/target pair.  May be null.
  */
  
  public String getToolTip(String sourceID, String targetID, String scaleKey) {
    //
    // Get the pert data:
    //
    
    List stPerts = getSrcTargPerts(sourceID, targetID);
    int numstp = stPerts.size();
    if (numstp == 0) {
      return ("");
    }
    
    //
    // FIXME: We are not returning data for proxies! (e.g. CAD for b-cat)
    //
    
    //
    // Extract what we want:
    //
      
    HashMap tipData = new HashMap();
    for (int i = 0; i < numstp; i++) {
      PertDataPoint pdp = (PertDataPoint)stPerts.get(i);
      
      String display = pdp.getScaledDisplayValueOldStyle(scaleKey, this, false);
      String batchKey = pdp.getDecoratedBatchKey(this, false);
      MinMax time = pdp.getTimeRange(this);
   
      SrcTarg stKey = new SrcTarg(pdp.getPertDisplayString(this, PertSources.NO_FOOTS), pdp.getTargetKey());
      TreeMap dataForST = (TreeMap)tipData.get(stKey);
      if (dataForST == null) {
        dataForST = new TreeMap();
        tipData.put(stKey, dataForST);
      }
      HashMap batchesForTime = (HashMap)dataForST.get(time);
      if (batchesForTime == null) {
        batchesForTime = new HashMap();
        dataForST.put(time, batchesForTime);
      }
      ArrayList dataForBatch = (ArrayList)batchesForTime.get(batchKey);
      if (dataForBatch == null) {
        dataForBatch = new ArrayList();
        batchesForTime.put(batchKey, dataForBatch);
      }
      dataForBatch.add(display);
    }
    
    //
    // Merge time points into time ranges, if needed:
    //
   
    Iterator tdit = tipData.keySet().iterator();
    while (tdit.hasNext()) {  
      SrcTarg stKey = (SrcTarg)tdit.next();   
      TreeMap dataForST = (TreeMap)tipData.get(stKey);
      Iterator dfstit = dataForST.keySet().iterator();
      TreeMap reducedForST = new TreeMap();
      while (dfstit.hasNext()) {  
        MinMax time = (MinMax)dfstit.next();
        if (time.min != time.max) {
          reducedForST.put(time, dataForST.get(time));
        }
      }
      dfstit = dataForST.keySet().iterator();
      while (dfstit.hasNext()) {  
        MinMax time = (MinMax)dfstit.next();
        HashMap batchesForTimeSrc = (HashMap)dataForST.get(time);
        if (time.min == time.max) {
          boolean transferred = false;
          Iterator redit = reducedForST.keySet().iterator();
          while (redit.hasNext()) {
            MinMax candidate = (MinMax)redit.next();
            if ((candidate.min != candidate.max) && candidate.contained(time.min)) {
              HashMap batchesForTimeTarg = (HashMap)reducedForST.get(candidate);              
              batchesForTimeTarg.putAll(batchesForTimeSrc);
              transferred = true;
              break;
            }
          }
          if (!transferred) {
            reducedForST.put(time, batchesForTimeSrc);
          }
        }
      }
      tipData.put(stKey, reducedForST);
    }
      
    //
    // Build the string
    //
  
    StringBuffer buf = new StringBuffer();
    buf.append("<html>");
    tdit = tipData.keySet().iterator();
    while (tdit.hasNext()) {  
      SrcTarg stKey = (SrcTarg)tdit.next();
      buf.append(stKey.srcID);
      buf.append(": ");
      buf.append(getTarget(stKey.targID));
      buf.append("<br>");        
      TreeMap dataForST = (TreeMap)tipData.get(stKey);
      Iterator dfstit = dataForST.keySet().iterator();
      while (dfstit.hasNext()) {  
        MinMax time = (MinMax)dfstit.next();
        buf.append("&nbsp;&nbsp;"); 
        buf.append(Experiment.getTimeDisplayString(appState_, time, true, true));   
        buf.append(": ");
        HashMap batchesForTime = (HashMap)dataForST.get(time);
        Iterator bftit = batchesForTime.keySet().iterator();
        while (bftit.hasNext()) {  
          String batchKey = (String)bftit.next();
          ArrayList dataForBatch = (ArrayList)batchesForTime.get(batchKey);
          Iterator dfbit = dataForBatch.iterator();
          while (dfbit.hasNext()) {  
            String dataStr = (String)dfbit.next();
            buf.append(dataStr);
            if (dfbit.hasNext()) {
              buf.append(",");
            }
          }
          if (bftit.hasNext()) {
            buf.append("/");
          }
        }
        buf.append("<br>");
      }
    }
    buf.append("</html>");    
    return (buf.toString());
  } 
 
  /***************************************************************************
  **
  ** Write the Perturbation data to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<perturbationData");
    if (serialNumber_ != 0L) {
      out.print(" serialNum=\"");
      out.print(serialNumber_);
      out.print("\"");
    }
    out.println(">");
    ind.up();
    
    //
    // get these out first, so they will be available when we read stuff
    // back in:
    //
     
    pertDict_.writeXML(out, ind);
    measureDict_.writeXML(out, ind);
    condDict_.writeXML(out, ind);
    
    if (pertAnnot_.haveMessages()) {
      pertAnnot_.writeXML(out, ind);
    }
    
    //
    // Targets
    //
    
    TreeSet tKeys = new TreeSet(targets_.keySet());
    if (!tKeys.isEmpty()) {
      ind.indent();
      out.println("<pertTargs>");
      ind.up();
      Iterator tkit = tKeys.iterator();
      while (tkit.hasNext()) {
        String tkey = (String)tkit.next();
        String targ = (String)targets_.get(tkey);
        ind.indent();
        out.print("<pertTarg id=\"");
        out.print(tkey);
        out.print("\" name=\"");
        out.print(CharacterEntityMapper.mapEntities(targ, false));
        List notes = getFootnotesForTarget(tkey);
        if ((notes != null) && !notes.isEmpty()) {
          out.print("\" notes=\"");
          out.print(Splitter.tokenJoin(notes, ","));
        }
        out.println("\"/>");
      }   
      ind.down().indent(); 
      out.println("</pertTargs>");
    }
    
    //
    // Source names
    //
    
    TreeSet snKeys = new TreeSet(sourceNames_.keySet());
    if (!snKeys.isEmpty()) {
      ind.indent();
      out.println("<pertSrcNames>");
      ind.up();
      Iterator tkit = snKeys.iterator();
      while (tkit.hasNext()) {
        String snkey = (String)tkit.next();
        String srcName = (String)sourceNames_.get(snkey);
        ind.indent();
        out.print("<pertSrcName id=\"");
        out.print(snkey);
        out.print("\" name=\"");
        out.print(CharacterEntityMapper.mapEntities(srcName, false));
        out.println("\"/>");
      }   
      ind.down().indent(); 
      out.println("</pertSrcNames>");
    }

    //
    // Investigators:
    //
    
    TreeSet iKeys = new TreeSet(investigators_.keySet());
    if (!iKeys.isEmpty()) {
      ind.indent();
      out.println("<pertInvests>");
      ind.up();
      Iterator ikit = iKeys.iterator();
      while (ikit.hasNext()) {
        String ikey = (String)ikit.next();
        String invest = (String)investigators_.get(ikey);
        ind.indent();
        out.print("<pertInvest id=\"");
        out.print(ikey);
        out.print("\" name=\"");
        out.print(CharacterEntityMapper.mapEntities(invest, false));
        out.println("\"/>");
      }   
      ind.down().indent(); 
      out.println("</pertInvests>");
    }
 
    //
    // Perturbation source definitions:
    //
    
    TreeSet sdKeys = new TreeSet(sourceDefs_.keySet());
    if (!sdKeys.isEmpty()) {
      ind.indent();
      out.println("<pertSourceDefs>");
      ind.up();
      Iterator sdkit = sdKeys.iterator();
      while (sdkit.hasNext()) {
        String sdkey = (String)sdkit.next();
        PertSource psd = (PertSource)sourceDefs_.get(sdkey);
        psd.writeXML(out, ind);
      }   
      ind.down().indent(); 
      out.println("</pertSourceDefs>");
    }
    
    //
    // Experiments:
    //  
    
    TreeSet psKeys = new TreeSet(experiments_.keySet());
    if (!psKeys.isEmpty()) {
      ind.indent();
      out.println("<experiments>");
      ind.up();
      Iterator pskit = psKeys.iterator();
      while (pskit.hasNext()) {
        String pskey = (String)pskit.next();
        Experiment psi = (Experiment)experiments_.get(pskey);
        psi.writeXML(out, ind);
      }   
      ind.down().indent(); 
      out.println("</experiments>");
    }
    
    //
    // Region restrictions:
    //  
    
    TreeSet rrKeys = new TreeSet(regionRestrictions_.keySet());
    if (!rrKeys.isEmpty()) {
      ind.indent();
      out.println("<regRestricts>");
      ind.up();
      Iterator rrkit = rrKeys.iterator();
      while (rrkit.hasNext()) {
        String rrkey = (String)rrkit.next();
        RegionRestrict rr = (RegionRestrict)regionRestrictions_.get(rrkey);
        rr.writeXML(out, ind, rrkey);
      }   
      ind.down().indent(); 
      out.println("</regRestricts>");
    }
    
    //
    // User-defined data fields:
    // 
    
    if (!userFields_.isEmpty()) { 
      ind.indent();
      out.println("<userFields>");
      ind.up();
      Iterator ufit = userFields_.iterator();
      while (ufit.hasNext()) {
        String fieldName = (String)ufit.next();
        ind.indent();
        out.print("<userField name=\"");
        out.print(CharacterEntityMapper.mapEntities(fieldName, false));
        out.println("\"/>");
      }   
      ind.down().indent(); 
      out.println("</userFields>");
 
      TreeSet edKeys = new TreeSet(userData_.keySet());
      if (!edKeys.isEmpty()) {
        ind.indent();
        out.println("<userDataVals>");
        ind.up();
        Iterator edkit = edKeys.iterator();
        while (edkit.hasNext()) {
          String udfkey = (String)edkit.next();
          ArrayList udf = (ArrayList)userData_.get(udfkey);
          writeXMLForUserData(out, ind, udf, udfkey);
        }   
        ind.down().indent(); 
        out.println("</userDataVals>");
      }
    }
    
    TreeSet dpKeys = new TreeSet(dataPoints_.keySet());
    if (!dpKeys.isEmpty()) {
      ind.indent();
      out.println("<dataPoints>");
      ind.up();
      Iterator dpkit = dpKeys.iterator();
      while (dpkit.hasNext()) {
        String dpkey = (String)dpkit.next();
        PertDataPoint pdp = (PertDataPoint)dataPoints_.get(dpkey);
        pdp.writeXML(out, ind, this);
      }
      ind.down().indent(); 
      out.println("</dataPoints>");
    } 
     
    boolean doEntry = entryMap_.haveData();
    boolean doSrc = sourceMap_.haveData();
    
    
    if (doEntry || doSrc) {
      ind.indent();
      out.println("<pertDataMaps>");
      ind.up();
      if (doEntry) {
        entryMap_.writeXML(out, ind);
      }
      if (doSrc) {
        sourceMap_.writeXML(out, ind);
      }
      ind.down().indent(); 
      out.println("</pertDataMaps>");
    }
    ind.down().indent();    
    out.println("</perturbationData>");
    return;
  }
  
  /***************************************************************************
  **
  ** Write out XML for user data.  Not really kosher using attributes, but trying
  ** to save space on possibly very expensive I/O:
  **
  */
  
  public void writeXMLForUserData(PrintWriter out, Indenter ind, List udf, String udfkey) {
    ind.indent();
    out.print("<userData key=\"");
    out.print(udfkey);
    out.print("\"");
    int numUDF = udf.size();
    for (int i = 0; i < numUDF; i++) {
      String datVal = (String)udf.get(i);
      if (datVal.trim().equals("")) {
        continue;
      }
      out.print(" f");
      out.print(i);
      out.print("=\"");
      out.print(CharacterEntityMapper.mapEntities(datVal, false));
      out.print("\"");
    }
    out.println("/>");
    return;
  }
  
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Used to return QPCR map results
  **
  */
  
  public static class QpcrMapResult {
    public String name;
    public int type;
    
    public static final int ENTRY_MAP  = 0;
    public static final int SOURCE_MAP = 1;

    public QpcrMapResult(String name, int type) {
      this.name = name;
      this.type = type;
    }
  }  

  /***************************************************************************
  **
  ** Used to return augmented QPCR changes
  **
  */
  
  public static class AugmentedQpcrChange {
    public Object other;
    public PertDataChange[] changes;
   
    public AugmentedQpcrChange(Object other, PertDataChange[] changes) {
      this.other = other;
      this.changes = changes;
    }
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the display string
  **
  */
  
  public static String getTimeDisplay(BTState appState, Integer timeObj, boolean showUnits, boolean abbreviate) {
    if (timeObj == null) {
      return (null);
    }

    TimeAxisDefinition tad = appState.getDB().getTimeAxisDefinition();
    String timeStr;
    if (tad.haveNamedStages()) {
      int timeNum = timeObj.intValue();
      TimeAxisDefinition.NamedStage stage = tad.getNamedStageForIndex(timeNum);
      timeStr = (abbreviate) ? stage.abbrev : stage.name;
    } else {
      timeStr = timeObj.toString();
    }
    
    if (!showUnits) {
      return (timeStr);
    }
    
    // E.g. "28 h"
    String displayUnitAbbrev = tad.unitDisplayAbbrev();    
    StringBuffer buf = new StringBuffer();
    if (tad.unitsAreASuffix()) {
      buf.append(timeStr);
      buf.append(" ");
      buf.append(displayUnitAbbrev);
    } else {
      buf.append(displayUnitAbbrev);      
      buf.append(" ");
      buf.append(timeStr);
    }
    return (buf.toString());
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Build up a full network of all interactions.  The links candidates returned
  ** have QPCR id tags in them, not genome ID tags.
  */
  
  private Set<LinkCandidate> buildFullInteractionNetwork(int collapseMode) {

    HashMap resultMap = new HashMap();
    Map[] collapseMaps = buildCollapseMaps();
 
    List sigPerts = getSignificantPerturbations();
    int numSig = sigPerts.size();
    for (int i = 0; i < numSig; i++) {
      PertDataPoint pdp = (PertDataPoint)sigPerts.get(i);
      int linkSign = pdp.getLink(this);
      if (linkSign == PertProperties.NO_LINK) {
        continue;
      }
      int stSign = mapDataSignToSignTag(linkSign);
      Experiment psi = pdp.getExperiment(this);
      Iterator sit = psi.getSources().getSources();
      while (sit.hasNext()) {
        String srcID = (String)sit.next();    
        PertSource src = getSourceDef(srcID);
        SrcTarg stk = new SrcTarg(src.getSourceNameKey(), pdp.getTargetKey());
        Integer vals = (Integer)resultMap.get(stk);
        if (vals == null) {
          vals = new Integer(stSign);
        } else {  
          vals = new Integer(vals.intValue() | stSign);
        }
        resultMap.put(stk, vals);
      }
    }

    //
    // Convert map to set:
    //
    
    HashSet<LinkCandidate> retval = new HashSet<LinkCandidate>();    
    Iterator rmit = resultMap.keySet().iterator();
    while (rmit.hasNext()) {
      SrcTarg stk = (SrcTarg)rmit.next();
      Integer vals = (Integer)resultMap.get(stk);
      collapseToCandidates(collapseMode, collapseMaps, stk, vals, retval);      
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Map measurement sign to sign tag
  */
  
  private int mapDataSignToSignTag(int sign) {   
    switch (sign) {
      case PertProperties.PROMOTE_LINK:
        return (PLUS_);
      case PertProperties.REPRESS_LINK:
        return (MINUS_);
      case PertProperties.UNSIGNED_LINK:
        return (UNDEFINED_);
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Applies collape maps and generates LinkCandidates
  */
  
  private void collapseToCandidates(int collapseMode, Map[] maps, SrcTarg stk, Integer vals, Set<LinkCandidate> retval) {    
    Map collapse = maps[collapseMode];

    int result = ((Integer)collapse.get(vals)).intValue();
    if ((result & PLUS_) != 0x00) {
      retval.add(new LinkCandidate(stk.srcID, stk.targID, Linkage.POSITIVE)); 
    } 
    if ((result & MINUS_) != 0x00) {
      retval.add(new LinkCandidate(stk.srcID, stk.targID, Linkage.NEGATIVE)); 
    }
    if ((result & UNDEFINED_) != 0x00) {
      retval.add(new LinkCandidate(stk.srcID, stk.targID, Linkage.NONE)); 
    }     
    return;
  }  
  
  /***************************************************************************
  **
  ** Collapses signs based on desired collapse mode.  Assumes at most one
  ** of each type (plus, minus, or unsigned).
  */
  
  private Map[] buildCollapseMaps() {  

    HashMap noCollapse = new HashMap();
    noCollapse.put(new Integer(NO_LINK_), new Integer(NO_LINK_));  
    noCollapse.put(new Integer(PLUS_), new Integer(PLUS_));
    noCollapse.put(new Integer(MINUS_), new Integer(MINUS_));
    noCollapse.put(new Integer(UNDEFINED_), new Integer(UNDEFINED_));
    noCollapse.put(new Integer(P_AND_M_), new Integer(P_AND_M_));
    noCollapse.put(new Integer(P_AND_U_), new Integer(P_AND_U_));
    noCollapse.put(new Integer(M_AND_U_), new Integer(M_AND_U_));
    noCollapse.put(new Integer(ALL_VALS_), new Integer(ALL_VALS_));  

    HashMap distinctPM = new HashMap();
    distinctPM.put(new Integer(NO_LINK_), new Integer(NO_LINK_));  
    distinctPM.put(new Integer(PLUS_), new Integer(PLUS_));
    distinctPM.put(new Integer(MINUS_), new Integer(MINUS_));
    distinctPM.put(new Integer(UNDEFINED_), new Integer(UNDEFINED_));
    distinctPM.put(new Integer(P_AND_M_), new Integer(P_AND_M_DISTINCT_PM_));
    distinctPM.put(new Integer(P_AND_U_), new Integer(P_AND_U_DISTINCT_PM_));
    distinctPM.put(new Integer(M_AND_U_), new Integer(M_AND_U_DISTINCT_PM_));
    distinctPM.put(new Integer(ALL_VALS_), new Integer(ALL_VALS_DISTINCT_PM_));  

    HashMap dropSign = new HashMap();
    dropSign.put(new Integer(NO_LINK_), new Integer(NO_LINK_));  
    dropSign.put(new Integer(PLUS_), new Integer(PLUS_));
    dropSign.put(new Integer(MINUS_), new Integer(MINUS_));
    dropSign.put(new Integer(UNDEFINED_), new Integer(UNDEFINED_));
    dropSign.put(new Integer(P_AND_M_), new Integer(P_AND_M_FULL_COLLAPSE_DROP_SIGN_));
    dropSign.put(new Integer(P_AND_U_), new Integer(P_AND_U_FULL_COLLAPSE_DROP_SIGN_));
    dropSign.put(new Integer(M_AND_U_), new Integer(M_AND_U_FULL_COLLAPSE_DROP_SIGN_));
    dropSign.put(new Integer(ALL_VALS_), new Integer(ALL_VALS_FULL_COLLAPSE_DROP_SIGN_));  

    HashMap keepSign = new HashMap();
    keepSign.put(new Integer(NO_LINK_), new Integer(NO_LINK_));  
    keepSign.put(new Integer(PLUS_), new Integer(PLUS_));
    keepSign.put(new Integer(MINUS_), new Integer(MINUS_));
    keepSign.put(new Integer(UNDEFINED_), new Integer(UNDEFINED_));
    keepSign.put(new Integer(P_AND_M_), new Integer(P_AND_M_FULL_COLLAPSE_KEEP_SIGN_));
    keepSign.put(new Integer(P_AND_U_), new Integer(P_AND_U_FULL_COLLAPSE_KEEP_SIGN_));
    keepSign.put(new Integer(M_AND_U_), new Integer(M_AND_U_FULL_COLLAPSE_KEEP_SIGN_));
    keepSign.put(new Integer(ALL_VALS_), new Integer(ALL_VALS_FULL_COLLAPSE_KEEP_SIGN_));

    HashMap[] retval = new HashMap[4];
    retval[NO_COLLAPSE] = noCollapse;
    retval[DISTINCT_PLUS_MINUS] = distinctPM;
    retval[FULL_COLLAPSE_DROP_SIGN] = dropSign;  
    retval[FULL_COLLAPSE_KEEP_SIGN] = keepSign;    

    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private class SrcTarg {
    String srcID;
    String targID;

    SrcTarg(String srcID, String targID) {
      this.srcID = srcID;
      this.targID = targID;
    }

    public String toString() {
      return (getSourceName(srcID) + " " + getTarget(targID));
    }
    
    public int hashCode() {
      return (srcID.hashCode() + targID.hashCode());
    }

    public boolean equals(Object other) {
      if (other == null) {
        return (false);
      }
      if (other == this) {
        return (true);
      }
      if (!(other instanceof SrcTarg)) {
        return (false);
      }
      SrcTarg otherST = (SrcTarg)other;

      if (!this.srcID.equals(otherST.srcID)) {
        return (false);
      }
      return (this.targID.equals(otherST.targID));
    }  
  }
  
  /***************************************************************************
  **
  ** Used to return QPCR sources
  **
  */
  
  public static class QpcrSourceSet {
    public String name;
    public int sourceCount;

    public QpcrSourceSet(String name, int sourceCount) {
      this.name = name;
      this.sourceCount = sourceCount;
    }
    
    public String toString() {
      return (name);
    }
    
    public int hashCode() {
      return (DataUtil.normKey(name).hashCode());
    }

    //
    // Two source names are equal if normalized versions equal:
    //
    
    public boolean equals(Object o) {
      if (this == o) {
        return (true);
      }
      if (!(o instanceof QpcrSourceSet)) {
        return (false);
      }
      QpcrSourceSet other = (QpcrSourceSet)o;
      if (this.sourceCount != other.sourceCount) {
        return (false);
      }
      if ((this.name == null) || (other.name == null)) {
        return ((this.name == null) && (other.name == null));  
      }
      return (DataUtil.normKey(this.name).equals(DataUtil.normKey(other.name)));
    }
  } 
  
  /***************************************************************************
  **
  ** For returning keys with undo info
  */  
  
  public static class KeyAndDataChange {
    public String key;
    public PertDataChange undoInfo;
    
    KeyAndDataChange(String key, PertDataChange undoInfo) {
      this.key = key;
      this.undoInfo = undoInfo;
    }  
  }
     
  /***************************************************************************
  **
  ** For region restrctions
  */  
  
  public static class RegionRestrict implements Cloneable {
    private boolean isNullTargetStyle_;
    private String valForNull_;
    private ArrayList<String> valsForRegular_;
    
    public RegionRestrict(String valForNull) {
      isNullTargetStyle_ = true;
      valForNull_ = valForNull;
    }
    
    public RegionRestrict(List<String> valsForRegular) {
      isNullTargetStyle_ = false;
      valsForRegular_ = new ArrayList<String>(valsForRegular);
    }
    
    public boolean isLegacyNullStyle() {
      return (isNullTargetStyle_);
    } 
    
    public String getLegacyValue() {
      if (!isNullTargetStyle_) {
        throw new IllegalStateException();
      }
      return (valForNull_);
    }
    
    public boolean containsRegionName(Set dropNames) {
      if (isNullTargetStyle_) {
        return (false);
      }
      int vfrn = valsForRegular_.size();
      for (int i = 0; i < vfrn; i++) {
        String vfr = (String)valsForRegular_.get(i);
        if (DataUtil.containsKey(dropNames, vfr)) {
          return (true);
        }
      }   
      return (false);
    } 
    
    //
    // Only legal to call if it contains the name, as 
    // we use null to indicate no regions left:
    //
       
    public RegionRestrict dropRegionName(String dropName) {
      if (isNullTargetStyle_) {
        throw new IllegalStateException();
      }
      ArrayList newVals = new ArrayList();
      boolean dropped = false;
      int vfrn = valsForRegular_.size();
      for (int i = 0; i < vfrn; i++) {
        String vfr = (String)valsForRegular_.get(i);
        if (!DataUtil.keysEqual(dropName, vfr)) {
          newVals.add(vfr);        
        } else {
          dropped = true;
        }
      }
      if (!dropped) {
        throw new IllegalStateException();
      }
      return ((newVals.isEmpty()) ? null : new RegionRestrict(newVals));
    } 
  
    public RegionRestrict changeRegionName(String oldName, String newName) {
      if (isNullTargetStyle_) {
        return (null);
      }
      ArrayList newVals = new ArrayList();
      boolean changed = false;
      int vfrn = valsForRegular_.size();
      for (int i = 0; i < vfrn; i++) {
        String vfr = (String)valsForRegular_.get(i);
        if (DataUtil.keysEqual(oldName, vfr)) {
          newVals.add(newName);
          changed = true;
        } else {
          newVals.add(vfr);
        }
      }   
      return ((changed) ? new RegionRestrict(newVals) : null);
    } 
 
    public void addRegion(String reg) {
      if (isNullTargetStyle_) {
        throw new IllegalStateException();
      }
      valsForRegular_.add(reg);
      return;
    } 
    
    public Iterator<String> getRegions() {
      if (isNullTargetStyle_) {
        throw new IllegalStateException();
      }
      return (valsForRegular_.iterator());
    } 
    
    public int numRegions() {
      if (isNullTargetStyle_) {
        throw new IllegalStateException();
      }
      return (valsForRegular_.size());
    } 
    
    public String getDisplayValue() {
      String retval = "";
      if (isLegacyNullStyle()) {
        retval = getLegacyValue();
      } else {
        StringBuffer buf = new StringBuffer();
        Iterator rrit = getRegions();
        while (rrit.hasNext()) {
          String reg = (String)rrit.next();
          buf.append(reg);
          if (rrit.hasNext()) {
            buf.append(", ");
          }
        }
        retval = buf.toString();
      }
      return (retval);
    }

    public RegionRestrict clone() {
      try {
        RegionRestrict newVal = (RegionRestrict)super.clone();
        if (this.valsForRegular_ != null) {
          // Strings in list don't need cloning:
          newVal.valsForRegular_ = (ArrayList)this.valsForRegular_.clone();
        }
        return (newVal);
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
     
    public void writeXML(PrintWriter out, Indenter ind, String key) {
      ind.indent();    
      out.print("<regRestrict dpKey=\"");
      out.print(key);
      if (isNullTargetStyle_) {
        out.print("\" descript=\"");
        out.print(CharacterEntityMapper.mapEntities(valForNull_, false));
        out.println("\"/>");
      } else {
        out.println("\">");
        ind.up();
        int numV = valsForRegular_.size();
        for (int i = 0; i < numV; i++) {          
          String val = (String)valsForRegular_.get(i);
          ind.indent();
          out.print("<reg name=\"");
          out.print(CharacterEntityMapper.mapEntities(val, false));
          out.println("\"/>");    
        }   
        ind.down().indent(); 
        out.println("</regRestrict>");
      }
      return;
    }
  }

  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class PertDataWorker extends AbstractFactoryClient {
    
    boolean mapsAreIllegal_;
    boolean serialNumberIsIllegal_;
    BTState appState_;
    
    public PertDataWorker(BTState appState, boolean mapsAreIllegal, boolean serialNumberIsIllegal) {
      super(new FactoryWhiteboard());
      appState_ = appState;
      FactoryWhiteboard whiteboard = (FactoryWhiteboard)sharedWhiteboard_;   
      myKeys_.add("perturbationData");
      mapsAreIllegal_ = mapsAreIllegal;
      serialNumberIsIllegal_ = serialNumberIsIllegal;
      installWorker(new PertDictionary.PertDictionaryWorker(whiteboard), new MyPertDictionaryGlue());
      installWorker(new MeasureDictionary.MeasureDictionaryWorker(whiteboard), new MyMeasureDictionaryGlue());
      installWorker(new ConditionDictionary.ConditionDictionaryWorker(whiteboard), new MyCondDictionaryGlue());
      installWorker(new PertAnnotations.PertAnnotationsWorker(whiteboard), new MyPertAnnotGlue());
      installWorker(new InvestWorker(whiteboard), new MyInvestGlue());
      installWorker(new TargetWorker(whiteboard), new MyAugTargGlue());
      installWorker(new SourceNameWorker(whiteboard), new MySrcNameGlue());    
      installWorker(new PertSource.PertSourceWorker(whiteboard), new MySourceDefGlue());
      installWorker(new Experiment.PertSourcesInfoWorker(appState_, whiteboard), new MySourceGlue());
      installWorker(new PertDataPoint.PertDataPointWorker(whiteboard), new MyDataGlue());
      installWorker(new NameMapper.NameMapperWorker(appState_, whiteboard), new MyNameMapperGlue());
      installWorker(new RegRestrictWorker(whiteboard), new MyRegResGlue());
      installWorker(new UserDataWorker(whiteboard), new MyUserDataGlue());
      installWorker(new UserFieldWorker(whiteboard), new MyUserFieldGlue());      
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("perturbationData")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.pertData = buildFromXML(elemName, attrs);
        retval = board.pertData;
        if (board.pertData != null) {
          appState_.getDB().setPertData(board.pertData);
        }
      }
      return (retval);     
    }  
    
    private PerturbationData buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String serNum = AttributeExtractor.extractAttribute(elemName, attrs, "perturbationData", "serialNum", false);
      if (serNum != null) {
        if (serialNumberIsIllegal_) {
          throw new IOException();
        }
        try {
          long sNum = Long.parseLong(serNum); 
          return (new PerturbationData(appState_, sNum));
        } catch (NumberFormatException nfex) {
          throw new IOException();
        }
      } else {
        return (new PerturbationData(appState_));
      }
    }
  }
  
   public static class MyInvestGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pertData.addInvestForIO(board.invest);
      return (null);
    }
  }  
   
  public static class MySrcNameGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pertData.addSourceNameForIO(board.srcName);
      return (null);
    }
  }   
  
  public static class MyAugTargGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pertData.addTargetForIO(board.augTarg);
      return (null);
    }
  }  
  
  public static class MyPertAnnotGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pertData.pertAnnot_ = board.pa;
      return (null);
    }
  }  

  public static class MySourceDefGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pertData.addPertSrcDefForIO(board.pertSrc);
      return (null);
    }
  }  
  
  public static class MySourceGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pertData.addPertSrcInfoForIO(board.pertSrcInfo);
      return (null);
    }
  }  

  public static class MyDataGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      PerturbationData pData = board.pertData;
      PertDataPoint.AugPertDataPoint apdp = board.augPertDataPt;
      pData.addDataPointForIO(apdp.pdp);      
      if ((apdp.notes != null) && !apdp.notes.isEmpty()) {
        pData.setFootnotesForDataPointForIO(apdp.pdp.getID(), apdp.notes);
      }
      return (null);
    }
  }  
  
  public static class MyNameMapperGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pertData.installNameMap(board.currPertDataMap);
      return (null);
    }
  } 
  
  public static class MyPertDictionaryGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pertData.pertDict_= board.pDict;
      return (null);
    }
  } 
  
   public static class MyCondDictionaryGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pertData.condDict_= board.cDict;
      return (null);
    }
  } 

  public static class MyMeasureDictionaryGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pertData.measureDict_ = board.mDict;
      return (null);
    }
  }
  
  public static class MyRegResGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      AugRegRestrict at = board.augRegRes;
      board.pertData.setRegionRestrictionForDataPointForIO(at.key, at.rr);
      return (null);
    }
  }
  
  public static class MyUserDataGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      AugUserData aug = board.augUserData;
      board.pertData.setUserDataForIO(aug.key, aug.indexToVal);
      return (null);
    }
  }
   
  public static class MyUserFieldGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      String fieldName = board.userFieldName;
      board.pertData.addUserFieldNameForIO(fieldName);
      return (null);
    }
  }
   
  /***************************************************************************
  **
  ** For XML I/O
  */  
     
  public static class AugRegRestrict {
    public String key;
    public RegionRestrict rr;
    
    public AugRegRestrict(String key, RegionRestrict rr) {
      this.key = key;
      this.rr = rr;
    }
  }
  
  public static class RegRestrictWorker extends AbstractFactoryClient {
 
    public RegRestrictWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("regRestrict");
     installWorker(new RegWorker(whiteboard), new MyRegGlue());
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("regRestrict")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.augRegRes = buildFromXML(elemName, attrs);
        retval = board.augRegRes;
      }
      return (retval);     
    }  

    private AugRegRestrict buildFromXML(String elemName, Attributes attrs) throws IOException {
      String key = AttributeExtractor.extractAttribute(elemName, attrs, "regRestrict", "dpKey", true);     
      String val = AttributeExtractor.extractAttribute(elemName, attrs, "regRestrict", "descript", false);
      RegionRestrict retval;
      if (val != null) {
        val = CharacterEntityMapper.unmapEntities(val, false);
        retval = new RegionRestrict(val);
      } else {
        retval = new RegionRestrict(new ArrayList());
      }
      return (new AugRegRestrict(key, retval));
    }
  }
  
  public static class RegWorker extends AbstractFactoryClient {
 
    public RegWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("reg");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("reg")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.reg = buildFromXML(elemName, attrs);
        retval = board.reg;
      }
      return (retval);     
    }  
    
    private String buildFromXML(String elemName, Attributes attrs) throws IOException {
      String regName = AttributeExtractor.extractAttribute(elemName, attrs, "reg", "name", true); 
      regName = CharacterEntityMapper.unmapEntities(regName, false);
      return (regName);
    }
  }
  
  public static class MyRegGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      RegionRestrict rr = board.augRegRes.rr;
      String reg = board.reg;
      try {
        rr.addRegion(reg);
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
      return (null);
    }
  } 
 
  /***************************************************************************
  **
  ** For XML I/O
  */  
     
  public static class AugTarget {
    public String id;
    public String name;
    public List<String> notes;
    
    public AugTarget(String id, String name, List<String> notes) {
      this.id = id;
      this.name = name;
      this.notes = notes;
    }
  }

  public static class TargetWorker extends AbstractFactoryClient {
 
    public TargetWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pertTarg");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pertTarg")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.augTarg = buildFromXML(elemName, attrs);
        retval = board.augTarg;
      }
      return (retval);     
    }  
    
    private AugTarget buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "pertTarg", "id", true);     
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "pertTarg", "name", true);
      name = CharacterEntityMapper.unmapEntities(name, false);
      String notes = AttributeExtractor.extractAttribute(elemName, attrs, "pertTarg", "notes", false);
    
      List<String> noteList = null;
      if (notes != null) {
        noteList = Splitter.stringBreak(notes, ",", 0, false);
      }      
      return (new AugTarget(id, name, noteList));
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
     
  public static class AugUserData {
    public String key;
    public Map indexToVal;
    
    public AugUserData(String key, Map indexToVal) {
      this.key = key;
      this.indexToVal = indexToVal;
    }
  }
 
  public static class UserDataWorker extends AbstractFactoryClient {
 
    public UserDataWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("userData");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("userData")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.augUserData = buildFromXML(elemName, attrs);
        retval = board.augUserData;
      }
      return (retval);     
    }  
    
    private AugUserData buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "userData", "key", true);
      HashMap indexToVal = new HashMap();
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if ((key == null) || key.equals("key") || (key.indexOf("f") != 0)) {
          continue;
        }
        
        String val = attrs.getValue(i);
        val = CharacterEntityMapper.unmapEntities(val, false);
        String indexStr = key.substring(1);
        indexToVal.put(indexStr, val);
      }
      return (new AugUserData(id, indexToVal));
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
     
  public static class UserFieldWorker extends AbstractFactoryClient {
 
    public UserFieldWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("userField");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("userField")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.userFieldName = buildFromXML(elemName, attrs);
        retval = board.userFieldName;
      }
      return (retval);     
    }  
    
    private String buildFromXML(String elemName, Attributes attrs) throws IOException {
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "userField", "name", true);
      name = CharacterEntityMapper.unmapEntities(name, false);
      return (name);
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
     
  public static class Invest {
    public String id;
    public String name;
    
    public Invest(String id, String name) {
      this.id = id;
      this.name = name;
    }
  }

  public static class InvestWorker extends AbstractFactoryClient {
 
    public InvestWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pertInvest");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pertInvest")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.invest = buildFromXML(elemName, attrs);
        retval = board.invest;
      }
      return (retval);     
    }  
    
    private Invest buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "pertInvest", "id", true);     
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "pertInvest", "name", true);
      name = CharacterEntityMapper.unmapEntities(name, false);
      return (new Invest(id, name));
    }
  }
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
     
  public static class SourceName {
    public String id;
    public String name;
    
    public SourceName(String id, String name) {
      this.id = id;
      this.name = name;
    }
  }

  public static class SourceNameWorker extends AbstractFactoryClient {
 
    public SourceNameWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("pertSrcName");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("pertSrcName")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.srcName = buildFromXML(elemName, attrs);
        retval = board.srcName;
      }
      return (retval);     
    }  
    
    private SourceName buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "pertSrcName", "id", true);     
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "pertSrcName", "name", true);
      name = CharacterEntityMapper.unmapEntities(name, false);
      return (new SourceName(id, name));
    }
  }
  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static class SerialNumberSet implements Cloneable {
    
    public long pDataSN;
    public long targMapSN;
    public long srcMapSN;
        
    public SerialNumberSet(long pDataSN, long targMapSN, long srcMapSN) {
      this.pDataSN = pDataSN;
      this.targMapSN = targMapSN;
      this.srcMapSN = srcMapSN;
    }
    
    public Object clone() {
      try {
        return ((SerialNumberSet)super.clone());
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    public boolean equals(Object o) {
      if (this == o) {
        return (true);
      }
      if (!(o instanceof SerialNumberSet)) {
        return (false);
      }
      SerialNumberSet other = (SerialNumberSet)o;      
      if (this.pDataSN != other.pDataSN) {
        return (false);
      }
      if (this.targMapSN != other.targMapSN) {
        return (false);
      }
      return (this.srcMapSN == other.srcMapSN);
    }
       
    public int hashCode() {
      return ((int)(pDataSN + targMapSN + srcMapSN));
    }
        
    public String getDisplayList() {
      ArrayList currNums = new ArrayList();
      currNums.add(Long.toString(pDataSN));
      currNums.add(Long.toString(targMapSN));
      currNums.add(Long.toString(srcMapSN));
      return (UiUtil.getListDisplay(currNums));
    }
    
    public void writeXML(PrintWriter out, Indenter ind) {
      ind.indent();
      UiUtil.xmlOpen(SN_SET_XML_TAG_, out, false);
      out.print("dataNum=\"");
      out.print(pDataSN);
      out.print("\" targNum=\"");
      out.print(targMapSN);
      out.print("\" srcNum=\"");
      out.print(srcMapSN);
      out.println("\" />");
    }
  }
  
  public static class SNSWorker extends AbstractFactoryClient {

    public SNSWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add(SN_SET_XML_TAG_);
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      if (elemName.equals(SN_SET_XML_TAG_)) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        String dNum = AttributeExtractor.extractAttribute(elemName, attrs, SN_SET_XML_TAG_, "dataNum", true);   
        String tNum = AttributeExtractor.extractAttribute(elemName, attrs, SN_SET_XML_TAG_, "targNum", true);   
        String sNum = AttributeExtractor.extractAttribute(elemName, attrs, SN_SET_XML_TAG_, "srcNum", true);
        
        try {
          long pDataSN = Long.parseLong(dNum);
          long targMapSN = Long.parseLong(tNum); 
          long srcMapSN = Long.parseLong(sNum);       
          SerialNumberSet sns = new SerialNumberSet(pDataSN, targMapSN, srcMapSN);
          board.sns = sns;
          return (sns);
        } catch (NumberFormatException nfex) {
          throw new IOException();
        }
      }
      return (null);     
    }
  }

  public static class SourceInfo implements Comparable {
    
    public String source;
    public int sign;
        
    public SourceInfo(String source, int sign) {
      this.source = source;
      this.sign = sign;
    }
    
    public int compareTo(Object o) {
      SourceInfo other = (SourceInfo)o;
      int srcCompare = source.compareTo(other.source);
      if (srcCompare != 0) {
        return (srcCompare);
      }
      return (this.sign - other.sign);
    }
    
    public boolean equals(Object o) {
      if (this == o) {
        return (true);
      }
      if (!(o instanceof SourceInfo)) {
        return (false);
      }
      SourceInfo other = (SourceInfo)o;      
      if (this.sign != other.sign) {
        return (false);
      }
      if (this.source == null) {
        return (other.source == null);
      }
      if (!this.source.equals(other.source)) {
        return (false);
      }
      return (true);
    }
       
    public int hashCode() {
      return ((source == null) ? 0 : source.hashCode());
    }
  } 
}
