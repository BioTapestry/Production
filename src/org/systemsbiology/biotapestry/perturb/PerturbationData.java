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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.systemsbiology.biotapestry.app.TabPinnedDynamicDataAccessContext;
import org.systemsbiology.biotapestry.cmd.undo.DisplayOptionsChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DataMapSource;
import org.systemsbiology.biotapestry.db.ExperimentalDataSource;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.qpcr.QpcrLegacyPublicExposed;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.DisplayOptionsChange;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.BoundedDoubMinMax;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.Splitter;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.UniqueLabeller;
import org.xml.sax.Attributes;

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
  
  public enum CollapseMode {NO_COLLAPSE,
                            DISTINCT_PLUS_MINUS,
                            FULL_COLLAPSE_DROP_SIGN,
                            FULL_COLLAPSE_KEEP_SIGN};
  
  
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
  private HashMap<String, String> invertSrcNameCache_;  
  
  private long invertTargNameCacheVersionSN_;
  private HashMap<String, String> invertTargNameCache_;
  
  private PertDisplayOptions pdo_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public PerturbationData() {
    labels_ = new UniqueLabeller();
    experiments_ = new HashMap<String, Experiment>();
    sourceDefs_ = new HashMap<String, PertSource>();
    dataPointNotes_ = new HashMap<String, List<String>>();
    userData_ = new HashMap<String, List<String>>();
    targetGeneNotes_ = new HashMap<String, List<String>>();
    dataPoints_ = new HashMap<String, PertDataPoint>();
    regionRestrictions_ = new HashMap<String, RegionRestrict>();
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
    
    pdo_ = new PertDisplayOptions(this);
  }

  /***************************************************************************
  **
  ** Constructor
  */

  public PerturbationData(long serNum) {
    this();
    serialNumber_ = serNum;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 

  /***************************************************************************
  **
  ** Get the display options for this data
  */
  
  public PertDisplayOptions getPertDisplayOptions() { 
    return (pdo_);
  }
  
  /***************************************************************************
  **
  ** Set the display options for this data
  */
  
  public void setLegacyPertDisplayOptions(PertDisplayOptions pdo) {
    pdo_ = pdo.clone();
    pdo_.setPerturbData(this);
    return;
  }
  
  /***************************************************************************
  **
  ** Set the display options for this data
  */
  
  public void setPertDisplayOptionsForIO(PertDisplayOptions pdo) {
    pdo_ = pdo;
    return;
  }
  
  
  /***************************************************************************
  ** 
  ** Install new display options
  */

  public void setPertDisplayOptions(PertDisplayOptions pdo, UndoSupport support, TabPinnedDynamicDataAccessContext tpdacx) {
    UiUtil.fixMePrintout("Cannot use single dacx_ support for all these changes!");
    DisplayOptionsChange doc = new DisplayOptionsChange();
    doc.oldPertOpts = pdo_.clone();
    pdo_ = pdo;
    doc.newPertOpts = pdo_.clone();    
    DisplayOptionsChangeCmd docc = new DisplayOptionsChangeCmd(doc, tpdacx);
    support.addEdit(docc);
    return;
  }
  
  /***************************************************************************
  ** 
  ** Do modifications if the perturbation data changes
  */

  public void modifyForPertDataChange(UndoSupport support, TabPinnedDynamicDataAccessContext tpdacx) {
    
    Map<String, String> revMap = pdo_.haveInconsistentMeasurementDisplayColors();
    String revSTag = pdo_.haveInconsistentScaleTag();
    if ((revMap != null) || (revSTag != null)) {
      PertDisplayOptions revDO = pdo_.clone();
      if (revMap != null) {
        revDO.setMeasurementDisplayColors(revMap);
      }
      if (revSTag != null) {      
        revDO.setPerturbDataDisplayScaleKey(revSTag);
      }
      DisplayOptionsChange doc = new DisplayOptionsChange();
      doc.oldPertOpts = pdo_.clone();
      pdo_ = revDO;
      doc.newPertOpts = pdo_.clone();    
      DisplayOptionsChangeCmd docc = new DisplayOptionsChangeCmd(doc, tpdacx);
      support.addEdit(docc);
    }
    return;   
  }

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
  
  public Vector<TrueObjChoiceContent> getExperimentOptions(TimeAxisDefinition tad) { 
    TreeSet<TrueObjChoiceContent> sorted = new TreeSet<TrueObjChoiceContent>();
    Iterator<String> oit = experiments_.keySet().iterator();
    while (oit.hasNext()) {
      String key = oit.next();
      sorted.add(getExperimentChoice(key, tad));
    }
    return (new Vector<TrueObjChoiceContent>(sorted));
  }  
  
  /***************************************************************************
  **
  ** Get the choice for an experiment
  */
  
  public TrueObjChoiceContent getExperimentChoice(String key, TimeAxisDefinition tad) {
    Experiment expr = experiments_.get(key);
    return (expr.getChoiceContent(this, tad));
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
    return (new DependencyAnalyzer(this));    
  } 
    
  /***************************************************************************
  **
  ** Transfer data from legacy QPCR into us
  */
  
  public void transferFromLegacy(DataMapSource dms, DisplayOptions dOpt, TimeAxisDefinition tad) {
    if (legacyQPCR_ != null) {
      pertDict_.createAllLegacyPerturbProps();
      legacyQPCR_.transferFromLegacy(this, dms, dOpt, tad);
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
  
  public Iterator<String> getUserFieldNames() {
    return (userFields_.iterator());
  } 
  
  /***************************************************************************
  **
  ** Get the set of names of the user-defined data fields:
  */
  
  public Set<String> getUserFieldNameSet() {
    return (new HashSet<String>(userFields_));
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
    return (userFields_.get(index));
  }
    
  /***************************************************************************
  **
  ** Gotta be able to invert.  Returns an Integer, or null:
  */
  
  public Integer getUserFieldIndexFromNameAsInt(String name) {
    String normName = DataUtil.normKey(name);
    int numf = userFields_.size();
    for (int i = 0; i < numf; i++) {
      String ufn = userFields_.get(i);
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
    List<String> list = userData_.get(pdpID);
    if (list == null) {
      return (null);
    }
    if (index >= list.size()) {
      return (null);
    }
    return (list.get(index));
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
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.USER_FIELD_VALS);
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.USER_FIELD_NAME);
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
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.USER_FIELD_NAME);

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
  
  public SortedMap<String, String> getPertAnnotationsMap() {
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
  
  private void stockQPCR(DBGenome dbGenome, TimeAxisDefinition tad, PerturbationDataMaps pdms, ResourceManager rMan) {
    if (legacyQPCR_ == null) {
      legacyQPCR_ = new QpcrLegacyPublicExposed();
    }
    long srcSN = pdms.getSerialNumber(PerturbationDataMaps.MapType.SOURCE);
    long trgSN = pdms.getSerialNumber(PerturbationDataMaps.MapType.ENTRIES);
    if (!legacyQPCR_.readyForDisplay() || 
        (qpcrForDisplayVersionSN_ != serialNumber_) ||
        (qpcrForDisplaySourceVersionSN_ != srcSN) ||
        (qpcrForDisplayEntryVersionSN_ != trgSN)) {
      legacyQPCR_.createQPCRFromPerts(this, tad, pdms, dbGenome, rMan);      
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
  
  public String getHTML(String geneId, String sourceID, boolean noCss, boolean bigScreen, 
                        DBGenome dbGenome, TimeAxisDefinition tad, PerturbationDataMaps pdms, ResourceManager rMan) {
    stockQPCR(dbGenome, tad, pdms, rMan);
    String ret = legacyQPCR_.getHTML(geneId, sourceID, noCss, bigScreen, dbGenome, this, tad, rMan);
    return (ret);
  }
  
  /***************************************************************************
  **
  ** Publish to file:
  */
  
  public boolean publish(PrintWriter out, DBGenome dbGenome, TimeAxisDefinition tad, PerturbationDataMaps pdms, ResourceManager rMan) {
    stockQPCR(dbGenome, tad, pdms, rMan);
    PertDisplayOptions dOpt = getPertDisplayOptions();
    return (legacyQPCR_.publish(out, this, tad, dOpt, rMan));
  }
  
  /***************************************************************************
  **
  ** Publish CSV to file:
  */
  
  public boolean publishAsCSV(PrintWriter out, TimeAxisDefinition tad) { //, TabPinnedDynamicDataAccessContext dacx) {
  
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
    //TimeAxisDefinition tad = appState_.getMetabase().getTimeAxisDefinition();   
    //String units = tad.unitDisplayString();
       
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
      buf.append(exp.getTimeDisplayString(tad, false, true));
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
        TreeSet<String> dates = new TreeSet<String>();
        TreeSet<String> measureScales = new TreeSet<String>();
        TreeSet<String> measureProps = new TreeSet<String>();
        TreeMap<String, SortedSet<String>> pointKeys = new TreeMap<String, SortedSet<String>>();
        TreeSet<String> controls = new TreeSet<String>();
       
        TreeSet<String> used = new TreeSet<String>();

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
            SortedSet<String> hs = pointKeys.get(ord);
            if (hs == null) {
              hs = new TreeSet<String>();
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
        
        Iterator<String> uit = used.iterator();
        while (uit.hasNext()) {
          String aKey = uit.next(); 
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
            
        Iterator<SortedSet<String>> pkit = pointKeys.values().iterator();
        while (pkit.hasNext()) {
          SortedSet<String> dpKeySet = pkit.next();
          Iterator<String> dpksit = dpKeySet.iterator();
          while (dpksit.hasNext()) {
            String dpKey = dpksit.next();
            PertDataPoint pdp = dataPoints_.get(dpKey);
            RegionRestrict rr = getRegionRestrictionForDataPoint(dpKey);
            pdp.publishAsCSV(out, this, exp, condDict_, measureDict_, pertAnnot_, rr, invests, tad);         
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
  
  public String getFootnoteListAsString(List<String> noteIDs) {
    return (pertAnnot_.getFootnoteListAsString(noteIDs));   
  }
  
  /***************************************************************************
  **
  ** Get a sorted, comma separated string of "footnote numbers = text" for the note IDs:
  */
  
  public String getFootnoteListAsNVString(List<String> noteIDs) {
    return (pertAnnot_.getFootnoteListAsNVString(noteIDs));   
  }
   
  /***************************************************************************
  **
  ** Get a sorted list of footnote numbers for the note IDs:
  */
  
  public List<String> getFootnoteList(List<String> noteIDs) {
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
  
  public Set<String> getTargetSet() {
    return (new HashSet<String>(targets_.values()));
  }

  
  /***************************************************************************
  **
  ** Get source names
  */
  
  public Map<String, String> getSourceNames() {
    return (sourceNames_);
  }
  
  /***************************************************************************
  **
  ** Get all the source names
  */
  
  public Set<String> getSourceNameSet() {
    return (new HashSet<String>(sourceNames_.values()));
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
    String retval = targets_.get(key);
    if (retval == null) {
      retval = sourceNames_.get(key);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get entry names
  */
  
  public Map<String, String> getEntryNames() {
    return (targets_);
  }

  /***************************************************************************
  **
  ** When building up networks of perturbation data, we need to be able to answer if
  ** a source name key and a target name key are the same
  */
  
  public boolean sourceTargetEquivalent(String sKey, String tKey) {
    String tName = targets_.get(tKey);
    String sName = sourceNames_.get(sKey);
    return (DataUtil.keysEqual(tName, sName));
  }

  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  public String getInvestKeyFromName(String name) {
    String normName = DataUtil.normKey(name);
    Iterator<String> ikit = investigators_.keySet().iterator();
    while (ikit.hasNext()) {
      String iKey = ikit.next();
      String inv = investigators_.get(iKey);
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
    Map<String, String> isnc = buildInvertSrcNameCache(); 
    return (isnc.get(normName));
  }  
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  private Map<String, String> buildInvertSrcNameCache() {
    if ((invertSrcNameCache_ == null) || (invertSrcNameCacheVersionSN_ != serialNumber_)) {
      invertSrcNameCache_ = new HashMap<String, String>();
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
    Map<String, String> itnc = buildInvertTrgNameCache();
    return (itnc.get(normName));
  }  
  
  /***************************************************************************
  **
  ** Gotta be able to invert:
  */
  
  private Map<String, String> buildInvertTrgNameCache() {
    if ((invertTargNameCache_ == null) || (invertTargNameCacheVersionSN_ != serialNumber_)) {
      invertTargNameCache_ = new HashMap<String, String>();
      Iterator<String> ikit = targets_.keySet().iterator();
      while (ikit.hasNext()) {
        String trgKey = ikit.next();
        String trg = targets_.get(trgKey);
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
    Iterator<String> ikit = targets_.keySet().iterator();
    while (ikit.hasNext()) {
      String trgKey = ikit.next();
      String trg = targets_.get(trgKey);
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
  
  public void setUserDataForIO(String pdID, Map<String, String> indexToVals) {
    ArrayList<String> myData = new ArrayList<String>();
    int numVals = userFields_.size();
    for (int i = 0; i < numVals; i++) {
      String strIndx = Integer.toString(i);
      String val = indexToVals.get(strIndx);
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.REG_RESTRICT);
    retval.dataRegResKey = pdpID;
    retval.dataRegResOrig = regionRestrictions_.get(pdpID);  // may be null
    if (regReg == null) {
      if (retval.dataRegResOrig == null) {  // no change...
        return (null);
      }
      retval.dataRegResOrig = retval.dataRegResOrig.clone();
      regionRestrictions_.remove(pdpID);
      retval.dataRegResNew = null;
    } else {
      retval.dataRegResOrig = (retval.dataRegResOrig == null) ? null : (RegionRestrict)retval.dataRegResOrig.clone();
      regionRestrictions_.put(pdpID, regReg.clone());
      retval.dataRegResNew = regReg.clone();
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
      RegionRestrict rro = undo.dataRegResOrig.clone();
      regionRestrictions_.put(undo.dataRegResKey, rro);
    } else {  // Change data
      RegionRestrict rro = undo.dataRegResOrig.clone();
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
      RegionRestrict rrn = redo.dataRegResNew.clone();
      regionRestrictions_.put(redo.dataRegResKey, rrn);
    } else if (redo.dataRegResNew == null) {  // Redo a delete
      regionRestrictions_.remove(redo.dataRegResKey);
    } else {  // Change data
      RegionRestrict rrn = redo.dataRegResNew.clone();
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
    return (regionRestrictions_.get(pdID));
  }
  
  /***************************************************************************
  **
  ** Change region name in region restrictions 
  */
  
  public PertDataChange[] changeRegionNameForRegionRestrictions(String oldName, String newName) {
    ArrayList<PertDataChange> allRets = new ArrayList<PertDataChange>();
    Iterator<String> rrit = regionRestrictions_.keySet().iterator();
    while (rrit.hasNext()) {
      String key = rrit.next();
      RegionRestrict regReg = regionRestrictions_.get(key);
      RegionRestrict changed = regReg.changeRegionName(oldName, newName);
      if (changed != null) {
        PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.REG_RESTRICT);
        retval.dataRegResKey = key;
        retval.dataRegResOrig = regReg.clone();
        regionRestrictions_.put(key, changed);
        retval.dataRegResNew = changed.clone();
        retval.serialNumberNew = ++serialNumber_;
        allRets.add(retval);
      }
    }
    PertDataChange[] changes = allRets.toArray(new PertDataChange[allRets.size()]);
    return (changes);
  }  
  
  /***************************************************************************
  **
  ** Drop region name in region restrictions 
  */
  
  public PertDataChange[] dropRegionNameForRegionRestrictions(String dropName) {
    ArrayList<PertDataChange> allRets = new ArrayList<PertDataChange>();
    HashSet<String> testSet = new HashSet<String>();
    testSet.add(dropName);
    Iterator<String> rrit = new HashSet<String>(regionRestrictions_.keySet()).iterator();
    while (rrit.hasNext()) {
      String key = rrit.next();
      RegionRestrict regReg = regionRestrictions_.get(key);
      if (!regReg.containsRegionName(testSet)) {
        continue;
      } 
      RegionRestrict dropped = regReg.dropRegionName(dropName);
      PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.REG_RESTRICT);
      retval.dataRegResKey = key;
      retval.dataRegResOrig = regReg.clone();
      if (dropped == null) {
        regionRestrictions_.remove(key);
        retval.dataRegResNew = null;
      } else {
        regionRestrictions_.put(key, dropped);
        retval.dataRegResNew = dropped.clone();
      }
      retval.serialNumberNew = ++serialNumber_;
      allRets.add(retval);
    }
    PertDataChange[] changes = allRets.toArray(new PertDataChange[allRets.size()]);
    return (changes);
  }  
  
  /***************************************************************************
  **
  ** Find region name in region restrictions 
  */
  
  public boolean haveRegionNameInRegionRestrictions(Set<String> dropNames) {
    Iterator<String> rrit = regionRestrictions_.keySet().iterator();
    while (rrit.hasNext()) {
      String key = rrit.next();
      RegionRestrict regReg = regionRestrictions_.get(key);
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
  
  public PertDataChange setFootnotesForDataPoint(String pdpID, List<String> noteMsgIDs) {
    if (pdpID == null) {
      throw new IllegalArgumentException();
    }   
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.DATA_ANNOTS);
    retval.dataAnnotsKey = pdpID;
    retval.dataAnnotsOrig = dataPointNotes_.get(pdpID);  // may be null
    if ((noteMsgIDs == null) || noteMsgIDs.isEmpty()) {
      if (retval.dataAnnotsOrig == null) {  // no change...
        return (null);
      }
      retval.dataAnnotsOrig = new ArrayList<String>(retval.dataAnnotsOrig);
      dataPointNotes_.remove(pdpID);
      retval.dataAnnotsNew = null;
    } else {
      retval.dataAnnotsOrig = (retval.dataAnnotsOrig == null) ? null : new ArrayList<String>(retval.dataAnnotsOrig);
      dataPointNotes_.put(pdpID, new ArrayList<String>(noteMsgIDs));
      retval.dataAnnotsNew = new ArrayList<String>(noteMsgIDs);
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
      List<String> dao = new ArrayList<String>(undo.dataAnnotsOrig);
      dataPointNotes_.put(undo.dataAnnotsKey, dao);
    } else {  // Change data
      List<String> dao = new ArrayList<String>(undo.dataAnnotsOrig);
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
      List<String> dan = new ArrayList<String>(redo.dataAnnotsNew);
      dataPointNotes_.put(redo.dataAnnotsKey, dan);
    } else if (redo.dataAnnotsNew == null) {  // Redo a delete
      dataPointNotes_.remove(redo.dataAnnotsKey);
    } else {  // Change data
      List<String> dan = new ArrayList<String>(redo.dataAnnotsNew);
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
  
  private PertDataChange setFootnotesForTarget(String targKey, List<String> noteMsgIDs) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.TARGET_ANNOTS);
    retval.targetAnnotKey = targKey;
    retval.targetAnnotOrig = targetGeneNotes_.get(targKey);  // may be null
    if ((noteMsgIDs == null) || noteMsgIDs.isEmpty()) {
      if (retval.targetAnnotOrig == null) {  // no change...
        return (null);
      }
      retval.targetAnnotOrig = new ArrayList<String>(retval.targetAnnotOrig);
      targetGeneNotes_.remove(targKey);
      retval.targetAnnotNew = null;
    } else {
      retval.targetAnnotOrig = (retval.targetAnnotOrig == null) ? null : new ArrayList<String>(retval.targetAnnotOrig);
      targetGeneNotes_.put(targKey, new ArrayList<String>(noteMsgIDs));
      retval.targetAnnotNew = new ArrayList<String>(noteMsgIDs);
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
      List<String> tao = new ArrayList<String>(undo.targetAnnotOrig);
      targetGeneNotes_.put(undo.targetAnnotKey, tao);
    } else {  // Change data
      List<String> tao = new ArrayList<String>(undo.targetAnnotOrig);
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
      List<String> tan = new ArrayList<String>(redo.targetAnnotNew);
      targetGeneNotes_.put(redo.targetAnnotKey, tan);
    } else if (redo.targetAnnotNew == null) {  // Redo a delete
      targetGeneNotes_.remove(redo.targetAnnotKey);
    } else {  // Change data
      List<String> tan = new ArrayList<String>(redo.targetAnnotNew);
      targetGeneNotes_.put(redo.targetAnnotKey, tan);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }   
  
  /***************************************************************************
  **
  ** Get footnotes for a target
  */
  
  public List<String> getFootnotesForTarget(String targKey) {
    return (targetGeneNotes_.get(targKey));
  }
  
  /***************************************************************************
  **
  ** Get target display string, with annotations:
  */
  
  public String getAnnotatedTargetDisplay(String targKey) {
    List<String> foots = targetGeneNotes_.get(targKey);
    String targ = targets_.get(targKey);
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
  
  public KeyAndDataChange provideExperiment(PertSources srcs, int time, int legacyMax, List<String> investList, String condKey) {
    Experiment prs = new Experiment("", srcs, time, investList, condKey);
    if (legacyMax != Experiment.NO_TIME) {
      prs.setLegacyMaxTime(legacyMax);
    }    
    Iterator<Experiment> prsit = experiments_.values().iterator();
    while (prsit.hasNext()) {
      Experiment chk = prsit.next();
      if (chk.equalsMinusID(prs)) {
        return (new KeyAndDataChange(chk.getID(), null));
      }
    }
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXPERIMENT);
    retval.expOrig = null;    
    String nextID = labels_.getNextLabel();
    prs.setID(nextID);
    retval.expNew = prs.clone();    
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

    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.SOURCE_NAME);
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
                                         List<String> annotations, boolean justSrcAndType) {
    PertSource ps = new PertSource("", srcNameKey, typeKey, annotations);
    if (!proxySign.equals(PertSource.NO_PROXY)) {
      ps.setProxiedSpeciesKey(proxyForKey);
      ps.setProxySign(proxySign);
    }
    Iterator<PertSource> sdit = sourceDefs_.values().iterator();
    while (sdit.hasNext()) {
      PertSource chk = sdit.next();
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
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.SOURCE_DEF);
    retval.srcDefOrig = null;  
    String nextID = labels_.getNextLabel();
    ps.setID(nextID);
    sourceDefs_.put(nextID, ps);
    retval.srcDefNew = ps.clone();
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.MEAS_SCALE);
    retval.mScaleOrig = null;
    String nextID = measureDict_.getNextDataKey();  
    MeasureScale scale = new MeasureScale(nextID, scaleDesc, convToFold, illegal, unchanged);
    measureDict_.setMeasureScale(scale);
    retval.mScaleNew = scale.clone();
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.MEASURE_PROP);
    retval.mpOrig = null;
    String nextID = measureDict_.getNextDataKey();  
    MeasureProps mProps = new MeasureProps(nextID, measName, scaleKey, negThresh, posThresh);
    measureDict_.setMeasureProp(mProps);
    retval.mpNew = mProps.clone();
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.PERT_PROP);
    retval.ppOrig = null;
    String nextID = pertDict_.getNextDataKey(); 
    PertProperties pProps = new PertProperties(nextID, propName, abbrev, linkRelation);
    pertDict_.setPerturbProp(pProps);
    retval.ppNew = pProps.clone();
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
      PertProperties ppr = undo.ppOrig.clone();
      pertDict_.addPerturbPropForIO(ppr);  // Handles ID accounting
    } else {  // Change data
      PertProperties ppr = undo.ppOrig.clone();
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
      PertProperties ppr = redo.ppNew.clone();
      pertDict_.addPerturbPropForIO(ppr);  // Handles ID accounting
    } else if (redo.ppNew == null) {  // Redo a delete
      String rescueID = redo.ppOrig.getID();
      pertDict_.dropPerturbProp(rescueID);
    } else {  // Change data
      PertProperties ppr = redo.ppNew.clone();
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXP_CONTROL);
    retval.ectrlOrig = null;
    String nextID = condDict_.getNextDataKey();  
    ExperimentControl ctrl = new ExperimentControl(nextID, ctrlDesc);
    condDict_.setExprControl(ctrl);
    retval.ectrlNew = ctrl.clone();
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXP_COND);
    retval.ecOrig = null;
    String nextID = condDict_.getNextDataKey();  
    ExperimentConditions eCond = new ExperimentConditions(nextID, condDesc);
    condDict_.setExprCondition(eCond);
    retval.ecNew = eCond.clone();
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.INVEST);
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.INVEST);
    retval.investOrig = investigators_.get(key);
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
  
  public PertDataChange mergeInvestigatorRefs(Set<String> expsToMerge, String investKey, Set<String> abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXPERIMENT_SET);
    retval.expSubsetOrig = new HashMap<String, Experiment>();
    retval.expSubsetNew = new HashMap<String, Experiment>();
    Iterator<String> k2dit = expsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = k2dit.next();
      Experiment exp = experiments_.get(key);
      List<String> investList = exp.getInvestigators();
      boolean haveAChange = false;
      int numInv = investList.size();
      ArrayList<String> replaceInvest = new ArrayList<String>();
      for (int i = 0; i < numInv; i++) {
        String chkKey = investList.get(i);
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
  
  public PertDataChange mergeSourceDefRefs(Set<String> expsToMerge, String sdefKey, Set<String> abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXPERIMENT_SET);
    retval.expSubsetOrig = new HashMap<String, Experiment>();
    retval.expSubsetNew = new HashMap<String, Experiment>();
    Iterator<String> k2dit = expsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = k2dit.next();
      Experiment exp = experiments_.get(key);
      ArrayList<String> replaceSources = new ArrayList<String>();
      boolean haveAChange = false;
      PertSources pss = exp.getSources();
      Iterator<String> pssit = pss.getSources();
      while (pssit.hasNext()) {
        String psid = pssit.next();
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
  
  public PertDataChange mergeExperimentCondRefs(Set<String> expsToMerge, String condKey, Set<String> abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXPERIMENT_SET);
    retval.expSubsetOrig = new HashMap<String, Experiment>();
    retval.expSubsetNew = new HashMap<String, Experiment>();
    Iterator<String> k2dit = expsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = k2dit.next();
      Experiment exp = experiments_.get(key);
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
  
  public PertDataChange mergeDataPointAnnotRefs(Set<String> dpToMerge, String annotKey, Set<String> abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.DATA_ANNOTS_SET);
    retval.dataPtNotesSubsetOrig = new HashMap<String, List<String>>();
    retval.dataPtNotesSubsetNew = new HashMap<String, List<String>>();
    Iterator<String> dpkit = dpToMerge.iterator();
    while (dpkit.hasNext()) {
      String key = dpkit.next();
      List<String> annots = dataPointNotes_.get(key);
      boolean haveAChange = false;
      int numA = annots.size();
      ArrayList<String> replaceAnnots = new ArrayList<String>();
      for (int i = 0; i < numA; i++) {
        String chkKey = annots.get(i);
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
        retval.dataPtNotesSubsetOrig.put(key, new ArrayList<String>(annots));
        dataPointNotes_.put(key, replaceAnnots);
        retval.dataPtNotesSubsetNew.put(key, new ArrayList<String>(replaceAnnots)); 
      }
    }
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Merge all target annotations to a single one
  */
  
  public PertDataChange mergeTargetAnnotRefs(Set<String> trgToMerge, String annotKey, Set<String> abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.TARG_ANNOTS_SET);
    retval.targetNotesSubsetOrig = new HashMap<String, List<String>>();
    retval.targetNotesSubsetNew = new HashMap<String, List<String>>();
    Iterator<String> trgkit = trgToMerge.iterator();
    while (trgkit.hasNext()) {
      String key = trgkit.next();
      List<String> annots = targetGeneNotes_.get(key);
      boolean haveAChange = false;
      int numA = annots.size();
      List<String> replaceAnnots = new ArrayList<String>();
      for (int i = 0; i < numA; i++) {
        String chkKey = annots.get(i);
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
        retval.targetNotesSubsetOrig.put(key, new ArrayList<String>(annots));
        targetGeneNotes_.put(key, replaceAnnots);
        retval.targetNotesSubsetNew.put(key, new ArrayList<String>(replaceAnnots)); 
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
    Iterator<String> dpnkit = undo.targetNotesSubsetOrig.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = dpnkit.next();
      List<String> dpn = undo.targetNotesSubsetOrig.get(key);
      List<String> dpnc =  new ArrayList<String>(dpn);
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
    Iterator<String> dpnkit = redo.targetNotesSubsetNew.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = dpnkit.next();
      List<String> dpn = redo.targetNotesSubsetNew.get(key);
      List<String> dpnc = new ArrayList<String>(dpn);
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
  
  public PertDataChange mergeSourceDefAnnotRefs(Set<String> sdToMerge, String annotKey, Set<String> abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.SOURCE_DEF_SET);
    retval.srcDefsSubsetOrig = new HashMap<String, PertSource>();
    retval.srcDefsSubsetNew = new HashMap<String, PertSource>();
 
    Iterator<String> sdit = sdToMerge.iterator();
    while (sdit.hasNext()) {
      String psdKey = sdit.next();
      PertSource chk = sourceDefs_.get(psdKey);
      List<String> annots = chk.getAnnotationIDs();
      boolean haveAChange = false;
      int numA = annots.size();
      List<String> replaceAnnots = new ArrayList<String>();
      for (int i = 0; i < numA; i++) {
        String chkKey = annots.get(i);
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
  
  public PertDataChange[] mergeInvestigatorNames(List<String> keyIDs, String useKey, String newName) {
    ArrayList<PertDataChange> allRets = new ArrayList<PertDataChange>();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.INVEST);
      String keyID = keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retval.investOrig = investigators_.get(keyID);
        retval.investKey = keyID;
        retval.investNew = newName;
        investigators_.put(keyID, newName);
      } else {
        retval.investOrig = investigators_.get(keyID);
        retval.investKey = keyID;
        retval.investNew = null;
        investigators_.remove(keyID);
        labels_.removeLabel(keyID);
      }
      retval.serialNumberNew = ++serialNumber_;
      allRets.add(retval);
    }    
    PertDataChange[] changes = allRets.toArray(new PertDataChange[allRets.size()]);
    return (changes);
  }

  /***************************************************************************
  **
  ** Delete an investigator.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deleteInvestigator(String keyID) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.INVEST);
    retval.investKey = keyID;
    retval.investOrig = investigators_.get(keyID);
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.ANNOTATION);
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.ANNOTATION);
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.ANNOTATION);
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
  
  public PertDataChange mergeAnnotations(List<String> joinKeys, String commonKey, String tag, String message) { 
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.ANNOT_MODULE);
    retval.pAnnotOrig = pertAnnot_.clone();
    pertAnnot_.mergeAnnotations(joinKeys, commonKey, tag, message);
    retval.pAnnotNew = pertAnnot_.clone();
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
    Iterator<String> ikit = targets_.keySet().iterator();
    while (ikit.hasNext()) {
      String trgKey = ikit.next();
      String trg = targets_.get(trgKey);
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.TARGET_NAME);
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
  
  public PertDataChange[] setTargetName(String keyID, String name, List<String> annots, List<PerturbationDataMaps> pdms, DBGenome dbGenome) {
    return (setTargetNameManageIdents(keyID, name, annots, true, pdms, dbGenome));
  }
  
  /***************************************************************************
  **
  ** Add or replace a target name, along with annotations
  */
  
  private PertDataChange[] setTargetNameManageIdents(String keyID, String name, List<String> annots, 
                                                     boolean doIdents, List<PerturbationDataMaps> pdms, DBGenome dbGenome) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();

    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.TARGET_NAME);
    retval.targetKey = keyID;
    retval.targetOrig = targets_.get(keyID);
    targets_.put(keyID, name); 
    retval.targetNew = name;
    retval.serialNumberNew = ++serialNumber_;
    retlist.add(retval);
      
    PertDataChange fnpdc = setFootnotesForTarget(keyID, annots);
    if (fnpdc != null) {
      retlist.add(fnpdc);
    }
    
    if (doIdents) {
      for (PerturbationDataMaps pdm : pdms) {
        retlist.addAll(Arrays.asList(pdm.dropIdentityMapsForEntries(dbGenome, targets_)));
      }
    }
        
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Add or replace a target name, along with annotations
  */
  
  private PertDataChange[] changeTargetNameManageIdents(String keyID, String name, List<PerturbationDataMaps> pdms, DBGenome dbGenome) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();

    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.TARGET_NAME);
    retval.targetKey = keyID;
    retval.targetOrig = targets_.get(keyID);
    targets_.put(keyID, name); 
    retval.targetNew = name;
    retval.serialNumberNew = ++serialNumber_;
    retlist.add(retval);
    for (PerturbationDataMaps pdm : pdms) {
      retlist.addAll(Arrays.asList(pdm.dropIdentityMapsForEntries(dbGenome, targets_)));
    }
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }

  /***************************************************************************
  **
  ** Delete a target name.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange[] deleteTargetName(String keyID, List<PerturbationDataMaps> pdatMaps) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
     
    PertDataChange fnpdc = setFootnotesForTarget(keyID, null);
    if (fnpdc != null) {
      retlist.add(fnpdc);
    }
    
    for (PerturbationDataMaps pdm : pdatMaps) {
      PertDataChange[] vals = pdm.dropDanglingMapsForEntry(keyID);
      for (int i = 0; i < vals.length; i++) {
        retlist.add(vals[i]);
      } 
    }
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.TARGET_NAME);
    retval.targetKey = keyID;
    retval.targetOrig = targets_.get(keyID);
    targets_.remove(keyID);
    labels_.removeLabel(keyID);
    retval.targetNew = null;
    retval.serialNumberNew = ++serialNumber_;
    retlist.add(retval);
   
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  
  /***************************************************************************
  **
  ** Merge a bunch of target names.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergeTargetNames(List<String> keyIDs, String useKey, String newName, 
                                           List<String> newFoots, List<PerturbationDataMaps> pdms, DBGenome dbGenome) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
    
    for (PerturbationDataMaps pdm : pdms) {
      retlist.addAll(Arrays.asList(pdm.mergeMapsForEntries(useKey, keyIDs)));
    }

    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.addAll(Arrays.asList(setTargetNameManageIdents(useKey, newName, newFoots, false, pdms, dbGenome)));
      } else {
        retlist.addAll(Arrays.asList(deleteTargetName(keyID, pdms)));
      }
    }
    for (PerturbationDataMaps pdm : pdms) {
      retlist.addAll(Arrays.asList(pdm.dropIdentityMapsForEntries(dbGenome, targets_)));
    }
    
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge a bunch of experimental controls.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergeExprControls(List<String> keyIDs, String useKey, ExperimentControl revisedECtrl) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setExperimentControl(revisedECtrl));
      } else {
        retlist.add(deleteExperimentControl(keyID));
      }
    }
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge a bunch of measurement props.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergeMeasureProps(List<String> keyIDs, String useKey, MeasureProps revisedMp) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setMeasureProp(revisedMp));        
      } else {
        retlist.add(deleteMeasureProp(keyID));    
      }
    }
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge a bunch of perturbation properties.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergePertProps(List<String> keyIDs, String useKey, PertProperties revisedPp) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setPerturbationProp(revisedPp));  
      } else {
        retlist.add(deletePerturbationProp(keyID));
      }
    }
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge a bunch of experimental conditions.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergeExprConditions(List<String> keyIDs, String useKey, ExperimentConditions revisedECond) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setExperimentConditions(revisedECond));
      } else {
        retlist.add(deleteExperimentConditions(keyID));
      }
    }
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }

  /***************************************************************************
  **
  ** Merge a bunch of source names.  Caller must have fixed up the refs first!
  */
  
  public PertDataChange[] mergeSourceNames(List<String> keyIDs, String useKey, String newName, 
                                           List<PerturbationDataMaps> pdms, DBGenome dbGenome) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
     
    // All nodes that have a custom map to one of the merged sources will now
    // map to the single merged source:
    //
    
    for (PerturbationDataMaps pdm : pdms) {
      retlist.addAll(Arrays.asList(pdm.mergeMapsForSources(useKey, keyIDs)));
    }
      
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.addAll(Arrays.asList(setSourceNameManageIdents(useKey, newName, false, pdms, dbGenome)));
      } else {
        retlist.addAll(Arrays.asList(deleteSourceName(keyID, pdms)));
      }
    }
    
    for (PerturbationDataMaps pdm : pdms) {
      retlist.addAll(Arrays.asList(pdm.dropIdentityMapsForSources(dbGenome, sourceNames_)));
    }
    
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }

  
  /***************************************************************************
  **
  ** Add or replace a source name
  */
  
  public PertDataChange[] setSourceName(String keyID, String name, List<PerturbationDataMaps> pdms, DBGenome dbGenome) {
    return (setSourceNameManageIdents(keyID, name, true, pdms, dbGenome));
  }
  
  /***************************************************************************
  **
  ** Add or replace a source name
  */
  
  private PertDataChange[] setSourceNameManageIdents(String keyID, String name, 
                                                     boolean dropIdent, List<PerturbationDataMaps> pdms, DBGenome dbGenome) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
    
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.SOURCE_NAME);
    retval.srcNameKey = keyID;
    retval.srcNameOrig = sourceNames_.get(keyID);
    sourceNames_.put(keyID, name); 
    retval.srcNameNew = name;
    retval.serialNumberNew = ++serialNumber_;
    retlist.add(retval);
    
    if (dropIdent) {
      for (PerturbationDataMaps pdm : pdms) {
        retlist.addAll(Arrays.asList(pdm.dropIdentityMapsForSources(dbGenome, sourceNames_)));
      }
    }
    
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }
   
  /***************************************************************************
  **
  ** Delete a source name.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange[] deleteSourceName(String keyID, List<PerturbationDataMaps> pdms) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
   
    for (PerturbationDataMaps pdm : pdms) {
      retlist.addAll(Arrays.asList(pdm.dropDanglingMapsForSource(keyID)));
    }

    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.SOURCE_NAME);
    retval.srcNameKey = keyID;
    retval.srcNameOrig = sourceNames_.get(keyID);
    sourceNames_.remove(keyID);
    labels_.removeLabel(keyID);
    retval.srcNameNew = null;
    retval.serialNumberNew = ++serialNumber_;
    retlist.add(retval);
    
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Add or replace a measurement property
  */
  
  public PertDataChange setMeasureProp(MeasureProps mprops) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.MEASURE_PROP);
    String key = mprops.getID();  
    MeasureProps orig = measureDict_.getMeasureProps(key);
    retval.mpOrig = (orig == null) ? null : (MeasureProps)orig.clone();
    measureDict_.setMeasureProp(mprops);
    retval.mpNew = mprops.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete a measurement property.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deleteMeasureProp(String key) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.MEASURE_PROP);
    MeasureProps orig = measureDict_.getMeasureProps(key);
    retval.mpOrig = orig.clone();
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
      MeasureProps mpo = undo.mpOrig.clone();
      measureDict_.addMeasurePropForIO(mpo);  // Handles ID accounting
    } else {  // Change data
      MeasureProps mpo = undo.mpOrig.clone();
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
      MeasureProps mpn = redo.mpNew.clone();
      measureDict_.addMeasurePropForIO(mpn);  // Handles ID accounting
    } else if (redo.mpNew == null) {  // Redo a delete
      String rescueID = redo.mpOrig.getID();
      measureDict_.dropMeasureProp(rescueID);
    } else {  // Change data
      MeasureProps mpn = redo.mpNew.clone();
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXP_COND);
    String key = expCond.getID();  
    ExperimentConditions orig = condDict_.getExprConditions(key);
    retval.ecOrig = (orig == null) ? null : orig.clone();
    condDict_.setExprCondition(expCond);
    retval.ecNew = expCond.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Delete an experimental condition.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deleteExperimentConditions(String key) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXP_COND);
    ExperimentConditions orig = condDict_.getExprConditions(key);
    retval.ecOrig = orig.clone();
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
      ExperimentConditions eco = undo.ecOrig.clone();
      condDict_.addExprConditionsForIO(eco);  // Handles ID accounting
    } else {  // Change data
      ExperimentConditions eco = undo.ecOrig.clone();
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
      ExperimentConditions ecn = redo.ecNew.clone();
      condDict_.addExprConditionsForIO(ecn);  // Handles ID accounting
    } else if (redo.ecNew == null) {  // Redo a delete
      String rescueID = redo.ecOrig.getID();
      condDict_.dropExprConditions(rescueID);
    } else {  // Change data
      ExperimentConditions ecn = redo.ecNew.clone();
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXP_CONTROL);
    String key = expCtrl.getID();  
    ExperimentControl orig = condDict_.getExprControl(key);
    retval.ectrlOrig = (orig == null) ? null : orig.clone();
    condDict_.setExprControl(expCtrl);
    retval.ectrlNew = expCtrl.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Delete an experimental control.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deleteExperimentControl(String key) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXP_CONTROL);
    ExperimentControl orig = condDict_.getExprControl(key);
    retval.ectrlOrig = orig.clone();
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
      ExperimentControl eco = undo.ectrlOrig.clone();
      condDict_.addExprControlForIO(eco);  // Handles ID accounting
    } else {  // Change data
      ExperimentControl eco = undo.ectrlOrig.clone();
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
      ExperimentControl ecn = redo.ectrlNew.clone();
      condDict_.addExprControlForIO(ecn);  // Handles ID accounting
    } else if (redo.ectrlNew == null) {  // Redo a delete
      String rescueID = redo.ectrlOrig.getID();
      condDict_.dropExprControl(rescueID);
    } else {  // Change data
      ExperimentControl ecn = redo.ectrlNew.clone();
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.MEAS_SCALE);
    String key = mScale.getID();  
    MeasureScale orig = measureDict_.getMeasureScale(key);
    retval.mScaleOrig = (orig == null) ? null : orig.clone();
    measureDict_.setMeasureScale(mScale);
    retval.mScaleNew = mScale.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Delete a measurement scale.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deleteMeasureScale(String key) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.MEAS_SCALE);
    MeasureScale orig = measureDict_.getMeasureScale(key);
    retval.mScaleOrig = orig.clone();
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
      MeasureScale mso = undo.mScaleOrig.clone();
      measureDict_.addMeasureScaleForIO(mso);  // Handles ID accounting
    } else {  // Change data
      MeasureScale mso = undo.mScaleOrig.clone();
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
      MeasureScale msn = redo.mScaleNew.clone();
      measureDict_.addMeasureScaleForIO(msn);  // Handles ID accounting
    } else if (redo.mScaleNew == null) {  // Redo a delete
      String rescueID = redo.mScaleOrig.getID();
      measureDict_.dropMeasureScale(rescueID);
    } else {  // Change data
      MeasureScale msn = redo.mScaleNew.clone();
      measureDict_.setMeasureScale(msn); 
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  } 

  /***************************************************************************
  **
  ** Merge all measurement scales refs to a single one
  */
  
  public PertDataChange mergeMeasureScaleRefs(Set<String> mPropsToMerge, String scaleKey) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.MEASURE_PROP_SET);
    retval.mPropsSubsetOrig = new HashMap<String, MeasureProps>();
    retval.mPropsSubsetNew = new HashMap<String, MeasureProps>();
    Iterator<String> k2dit = mPropsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = k2dit.next();
      MeasureProps mp = measureDict_.getMeasureProps(key);
      if (!scaleKey.equals(mp.getScaleKey())) {
        retval.mPropsSubsetOrig.put(key, mp.clone());
        mp = mp.clone();
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
    Iterator<String> dpnkit = undo.mPropsSubsetOrig.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = dpnkit.next();
      MeasureProps mp = undo.mPropsSubsetOrig.get(key);
      MeasureProps mpc = mp.clone();
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
    Iterator<String> dpnkit = redo.mPropsSubsetNew.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = dpnkit.next();
      MeasureProps mp = redo.mPropsSubsetNew.get(key);
      MeasureProps mpc = mp.clone();
      measureDict_.setMeasureProp(mpc);
    }
    serialNumber_ = redo.serialNumberNew;
    return;
  }     

  /***************************************************************************
  **
  ** Merge measurement scales.  Caller must handle merged references first!
  */
  
  public PertDataChange[] mergeMeasureScales(List<String> keyIDs, String useKey, MeasureScale scale) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = keyIDs.get(i);
      if (keyID.equals(useKey)) {
        if (!scale.getID().equals(useKey)) {
          throw new IllegalArgumentException();
        }
        retlist.add(setMeasureScale(scale.clone()));
      } else {
        retlist.add(deleteMeasureScale(keyID));
      }
    }
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }

  /***************************************************************************
  **
  ** Add or replace a perturbation property
  */
  
  public PertDataChange setPerturbationProp(PertProperties pprops) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.PERT_PROP);
    String key = pprops.getID();  
    PertProperties orig = pertDict_.getPerturbProps(key);
    retval.ppOrig = (orig == null) ? null : orig.clone();
    pertDict_.setPerturbProp(pprops);
    retval.ppNew = pprops.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete a perturbation property.  Caller must make sure nobody references
  ** it before it goes!
  */
  
  public PertDataChange deletePerturbationProp(String key) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.PERT_PROP);
    PertProperties orig = pertDict_.getPerturbProps(key);
    retval.ppOrig = orig.clone();
    pertDict_.dropPerturbProp(key);
    retval.ppNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public PerturbationData clone() {
    try {
      PerturbationData newVal = (PerturbationData)super.clone();
      
      newVal.dataPoints_ = new HashMap<String, PertDataPoint>();
      Iterator<String> dpit = this.dataPoints_.keySet().iterator();
      while (dpit.hasNext()) {
        String dpKey = dpit.next();
        newVal.dataPoints_.put(dpKey, this.dataPoints_.get(dpKey).clone());
      }
      
      newVal.sourceDefs_ = new HashMap<String, PertSource>();
      Iterator<String> sdit = this.sourceDefs_.keySet().iterator();
      while (sdit.hasNext()) {
        String sdKey = sdit.next();
        newVal.sourceDefs_.put(sdKey, this.sourceDefs_.get(sdKey).clone());
      }
      
      newVal.experiments_ = new HashMap<String, Experiment>();
      Iterator<String> psit = this.experiments_.keySet().iterator();
      while (psit.hasNext()) {
        String psKey = psit.next();
        newVal.experiments_.put(psKey, this.experiments_.get(psKey).clone());
      }
      
      newVal.dataPointNotes_ = new HashMap<String, List<String>>();
      Iterator<String> dpnit = this.dataPointNotes_.keySet().iterator();
      while (dpnit.hasNext()) {
        String dpKey = dpnit.next();
        newVal.dataPointNotes_.put(dpKey, new ArrayList<String>(this.dataPointNotes_.get(dpKey)));
      } 
      
      newVal.userData_ = new HashMap<String, List<String>>();
      Iterator<String> udit = this.userData_.keySet().iterator();
      while (udit.hasNext()) {
        String udKey = udit.next();
        // Data is immutable, shallow copy is OK:
        newVal.userData_.put(udKey, new ArrayList<String>(this.dataPointNotes_.get(udKey)));
      } 
 
      newVal.pertAnnot_ = this.pertAnnot_.clone();
      newVal.labels_ = this.labels_.clone();
      
      newVal.investigators_ = new HashMap<String, String>(this.investigators_);
      newVal.targets_ = new HashMap<String, String>(this.targets_);
      newVal.sourceNames_ = new HashMap<String, String>(this.sourceNames_);

      newVal.regionRestrictions_ = new HashMap<String, RegionRestrict>();
      Iterator<String> rrit = this.regionRestrictions_.keySet().iterator();
      while (rrit.hasNext()) {
        String rKey = rrit.next();
        newVal.regionRestrictions_.put(rKey, this.regionRestrictions_.get(rKey).clone());
      }
  
      newVal.pertDict_ = this.pertDict_.clone();
      newVal.measureDict_ = this.measureDict_.clone();
      newVal.condDict_ = this.condDict_.clone();
      newVal.userFields_ = new ArrayList<String>(this.userFields_);
 
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
    String invest = investigators_.get(key);
    return (new TrueObjChoiceContent(invest, key));    
  }
  
  /***************************************************************************
  **
  ** Get ALL the candidate values for the given single filter category (not
  ** just those present in a given set of data points)
  */
  
  public SortedSet<TrueObjChoiceContent> getAllAvailableCandidates(PertFilter.Cat filterCat, TimeAxisDefinition tad) {
    TreeSet<TrueObjChoiceContent> retval = new TreeSet<TrueObjChoiceContent>();
    switch (filterCat) {
      case EXPERIMENT:
        Iterator<Experiment> prsit = experiments_.values().iterator();
        while (prsit.hasNext()) {
          Experiment chk = prsit.next();
          chk.addToExperimentSet(retval, this, tad);
        }
        return (retval);
      case TARGET:
        Iterator<String> tkit = targets_.keySet().iterator();
        while (tkit.hasNext()) {
          String tkey = tkit.next();
          retval.add(getTargetChoiceContent(tkey, true));
        }
        return (retval);    
      case SOURCE_OR_PROXY_NAME:
        Iterator<String> snkit = sourceNames_.keySet().iterator();
        while (snkit.hasNext()) {
          String snkey = snkit.next();
          retval.add(getSourceOrProxyNameChoiceContent(snkey));
        }
        return (retval);      
      case INVEST:
        Iterator<String> invkit = investigators_.keySet().iterator();
        while (invkit.hasNext()) {
          String ikey = invkit.next();
          retval.add(getInvestigatorChoiceContent(ikey));
        }
        return (retval); 
      case SOURCE_NAME:
      case SOURCE:
      case PERT:
      case TIME:
      case VALUE:
      case INVEST_LIST:
      case EXP_CONTROL:
      case EXP_CONDITION:
      case MEASURE_SCALE:
      case ANNOTATION:
      case MEASURE_TECH:
      default:
        throw new IllegalArgumentException();
    }    
  }  
  
  /***************************************************************************
  **
  ** Get the candidate values for the given single filter category
  */
  
  public SortedSet<TrueObjChoiceContent> getCandidates(List<PertDataPoint> chooseFrom, 
                                                       PertFilter.Cat filterCat, TimeAxisDefinition tad, ResourceManager rMan) {
    TreeSet<TrueObjChoiceContent> retval = new TreeSet<TrueObjChoiceContent>();
    if (chooseFrom == null) {
      chooseFrom = new ArrayList<PertDataPoint>(dataPoints_.values());
    }
    int numRs = chooseFrom.size();
    for (int i = 0; i < numRs; i++) {
      PertDataPoint pdp = chooseFrom.get(i);
      pdp.getCandidates(filterCat, retval, this, tad, rMan);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the candidate values for the given single filter category
  */
  
  public SortedSet<TrueObjChoiceContent> getCandidates(PertFilter.Cat filterCat, TimeAxisDefinition tad, ResourceManager rMan) {
    return (getCandidates(null, filterCat, tad, rMan));
  }
   
  /***************************************************************************
  **
  ** Get all the perturbation data as filtered
  */
  
  public List<PertDataPoint> getPerturbations(PertFilterExpression pfe) {
    TreeMap<String, PertDataPoint> sorted = new TreeMap<String, PertDataPoint>(dataPoints_);
    TreeSet<String> sortedKeys = new TreeSet<String>(sorted.keySet());
    
    ArrayList<PertDataPoint> retval = new ArrayList<PertDataPoint>();
    SortedSet<String> result = pfe.getFilteredResult(sortedKeys, sorted, this);
    Iterator<String> resIt = result.iterator();
    while (resIt.hasNext()) {
      String pdpID = resIt.next();
      retval.add(sorted.get(pdpID));
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
    return (sourceDefs_.get(key));
  }

  /***************************************************************************
  **
  ** Get the annotations for a data point
  */ 
  
  public List<String> getDataPointNotes(String dpkey) {
    return (dataPointNotes_.get(dpkey));
  }
  
  /***************************************************************************
  **
  ** Get all keys for dp notes
  */ 
  
  public Iterator<String> getDataPointNoteKeys() {
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
  
  public Iterator<PertDataPoint> getDataPoints() {
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
      Iterator<String> kit = undo.dataPtsSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = kit.next();
        PertDataPoint pdp = undo.dataPtsSubsetOrig.get(key);
        PertDataPoint pdpc = pdp.clone();
        dataPoints_.put(key, pdpc);
        labels_.addExistingLabel(key);
      }
      // Restore region restrictions:
      if (undo.dataPtRegResSubsetOrig != null) {
        Iterator<String> rrkit = undo.dataPtRegResSubsetOrig.keySet().iterator();
        while (rrkit.hasNext()) {
          String key = rrkit.next();
          RegionRestrict rr = undo.dataPtRegResSubsetOrig.get(key);
          RegionRestrict rrc = rr.clone();
          regionRestrictions_.put(key, rrc);
        }
      }
      // Restore user data:
      if (undo.userDataSubsetOrig != null) {
        Iterator<String> udkit = undo.userDataSubsetOrig.keySet().iterator();
        while (udkit.hasNext()) {
          String key = udkit.next();
          List<String> ud = undo.userDataSubsetOrig.get(key);
          List<String> udc = new ArrayList<String>(ud);
          userData_.put(key, udc);
        }
      }
      //  Restore note list:
      if (undo.dataPtNotesSubsetOrig != null) {
        Iterator<String> dpnkit = undo.dataPtNotesSubsetOrig.keySet().iterator();
        while (dpnkit.hasNext()) {
          String key = dpnkit.next();
          List<String> dpn = undo.dataPtNotesSubsetOrig.get(key);
          List<String> dpnc = new ArrayList<String>(dpn);
          dataPointNotes_.put(key, dpnc);
        }
      }
  
    } else {  // Change data
      Iterator<String> kit = undo.dataPtsSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = kit.next();
        PertDataPoint pdp = undo.dataPtsSubsetOrig.get(key);
        PertDataPoint pdpc = pdp.clone();
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
      Iterator<String> kit = redo.dataPtsSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = kit.next();
        dataPoints_.remove(key);
        labels_.removeLabel(key);
      }
      // Delete region restrictions:
      if (redo.dataPtRegResSubsetOrig != null) {
        Iterator<String> rrkit = redo.dataPtRegResSubsetOrig.keySet().iterator();
        while (rrkit.hasNext()) {
          String key = rrkit.next();
          regionRestrictions_.remove(key);
        }
      }
      // Delete user data:
      if (redo.userDataSubsetOrig != null) {
        Iterator<String> udkit = redo.userDataSubsetOrig.keySet().iterator();
        while (udkit.hasNext()) {
          String key = udkit.next();
          userData_.remove(key);
        }
      }
      //  Delete note list:
      if (redo.dataPtNotesSubsetOrig != null) {
        Iterator<String> dpnkit = redo.dataPtNotesSubsetOrig.keySet().iterator();
        while (dpnkit.hasNext()) {
          String key = dpnkit.next();
          dataPointNotes_.remove(key);
        }
      } 
    } else {  // Redo Change data
      Iterator<String> kit = redo.dataPtsSubsetNew.keySet().iterator();
      while (kit.hasNext()) {
        String key = kit.next();
        PertDataPoint pdp = redo.dataPtsSubsetNew.get(key);
        PertDataPoint pdpc = pdp.clone();
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
  
  public PertDataChange deleteDataPoints(Set<String> keysToDrop) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.DATA_POINT_SET);
    retval.dataPtsSubsetOrig = new HashMap<String, PertDataPoint>();
    Iterator<String> k2dit = keysToDrop.iterator();
    while (k2dit.hasNext()) {
      String key = k2dit.next();
      // Delete region restrict:
      RegionRestrict regReg = regionRestrictions_.get(key); 
      if (regReg != null) {
        if (retval.dataPtRegResSubsetOrig == null) {
          retval.dataPtRegResSubsetOrig = new HashMap<String, RegionRestrict>();
        }
        retval.dataPtRegResSubsetOrig.put(key, regReg.clone());
        regionRestrictions_.remove(key);
      }
      // Delete user data:
      List<String> userDataList = userData_.get(key); 
      if (userDataList != null) {
        if (retval.userDataSubsetOrig == null) {
          retval.userDataSubsetOrig = new HashMap<String, List<String>>();
        }
        retval.userDataSubsetOrig.put(key, new ArrayList<String>(userDataList));
        userData_.remove(key);
      }
      // Delete note list:
      List<String> noteList = dataPointNotes_.get(key); 
      if (noteList != null) {
        if (retval.dataPtNotesSubsetOrig == null) {
          retval.dataPtNotesSubsetOrig = new HashMap<String, List<String>>();
        }
        retval.dataPtNotesSubsetOrig.put(key, new ArrayList<String>(noteList));
        dataPointNotes_.remove(key);
      }
      // Delete the data:  
      PertDataPoint pdp = dataPoints_.get(key);
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
  
  public PertDataChange mergeDataPointTargetRefs(Set<String> pointsToMerge, String targKey) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.DATA_POINT_SET);
    retval.dataPtsSubsetOrig = new HashMap<String, PertDataPoint>();
    retval.dataPtsSubsetNew = new HashMap<String, PertDataPoint>();
    Iterator<String> k2dit = pointsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = k2dit.next();
      PertDataPoint pdp = dataPoints_.get(key);
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
  
  public PertDataChange mergeDataPointControlRefs(Set<String> pointsToMerge, String ctrlKey) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.DATA_POINT_SET);
    retval.dataPtsSubsetOrig = new HashMap<String, PertDataPoint>();
    retval.dataPtsSubsetNew = new HashMap<String, PertDataPoint>();
    Iterator<String> k2dit = pointsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = k2dit.next();
      PertDataPoint pdp = dataPoints_.get(key);
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
  
  public PertDataChange mergeMeasurePropRefs(Set<String> pointsToMerge, String mpKey) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.DATA_POINT_SET);
    retval.dataPtsSubsetOrig = new HashMap<String, PertDataPoint>();
    retval.dataPtsSubsetNew = new HashMap<String, PertDataPoint>();
    Iterator<String> k2dit = pointsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = k2dit.next();
      PertDataPoint pdp = dataPoints_.get(key);
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
  
  public PertDataChange deleteExperiments(Set<String> keysToDrop) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXPERIMENT_SET);    
    retval.expSubsetOrig = new HashMap<String, Experiment>();
    Iterator<String> k2dit = keysToDrop.iterator();
    while (k2dit.hasNext()) {
      String dropkey = k2dit.next();
      Experiment psi = experiments_.get(dropkey);
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
  
  public PertDataChange dropInvestigator(String investID, Set<String> keysToPrune) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXPERIMENT_SET);   
    retval.expSubsetOrig = new HashMap<String, Experiment>();
    retval.expSubsetNew = new HashMap<String, Experiment>();
    Iterator<String> k2dit = keysToPrune.iterator();
    while (k2dit.hasNext()) {
      String pruneKey = k2dit.next();
      Experiment psi = experiments_.get(pruneKey);
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
  
  public PertDataChange dropDataPointAnnotations(String annotID, Set<String> keysToPrune) {
    PertDataChange retval = null;
    Iterator<String> kpit = keysToPrune.iterator();
    while (kpit.hasNext()) {
      String dpkey = kpit.next();
      List<String> noteList = dataPointNotes_.get(dpkey); 
      if (noteList != null) {
        if (retval == null) {
          retval = new PertDataChange(serialNumber_, PertDataChange.Mode.DATA_ANNOTS_SET);  
          retval.dataPtNotesSubsetOrig = new HashMap<String, List<String>>();
          retval.dataPtNotesSubsetNew = new HashMap<String, List<String>>();
        }
        retval.dataPtNotesSubsetOrig.put(dpkey, new ArrayList<String>(noteList));
        noteList.remove(annotID);
        if (noteList.isEmpty()) {
          dataPointNotes_.remove(dpkey); 
        }
        // Empty list MUST cause deletion on redo
        retval.dataPtNotesSubsetNew.put(dpkey, new ArrayList<String>(noteList)); 
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
    Iterator<String> dpnkit = undo.dataPtNotesSubsetOrig.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = dpnkit.next();
      List<String> dpn = undo.dataPtNotesSubsetOrig.get(key);
      List<String> dpnc = new ArrayList<String>(dpn);
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
    Iterator<String> dpnkit = redo.dataPtNotesSubsetNew.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = dpnkit.next();
      List<String> dpn = redo.dataPtNotesSubsetNew.get(key);
      List<String> dpnc = new ArrayList<String>(dpn);
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
  
  public PertDataChange dropDataPointControls(Set<String> keysToPrune) {
    PertDataChange retval = null;
    Iterator<String> kpit = keysToPrune.iterator();
    while (kpit.hasNext()) {
      String dpkey = kpit.next();
      PertDataPoint pdp = dataPoints_.get(dpkey); 
      if (retval == null) {
        retval = new PertDataChange(serialNumber_, PertDataChange.Mode.DATA_POINT_SET);  
        retval.dataPtsSubsetOrig = new HashMap<String, PertDataPoint>();
        retval.dataPtsSubsetNew = new HashMap<String, PertDataPoint>();
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
  
  public PertDataChange dropSourceDefAnnotations(String annotID, Set<String> keysToPrune) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.SOURCE_DEF_SET);
    
    retval.srcDefsSubsetOrig = new HashMap<String, PertSource>();
    Iterator<String> psdit = keysToPrune.iterator();
    while (psdit.hasNext()) {
      String sdkey = psdit.next();
      PertSource ps = sourceDefs_.get(sdkey);
      retval.srcDefsSubsetOrig.put(sdkey, ps.clone());
    }
    
    Iterator<String> k2dit = keysToPrune.iterator();
    while (k2dit.hasNext()) {
      String pruneKey = k2dit.next();
      PertSource ps = sourceDefs_.get(pruneKey);
      List<String> psan = new ArrayList<String>(ps.getAnnotationIDs());
      psan.remove(annotID);    
      ps.setAnnotationIDs(psan);
    }
    
    retval.srcDefsSubsetNew = new HashMap<String, PertSource>();
    psdit = keysToPrune.iterator();
    while (psdit.hasNext()) {
      String sdkey = psdit.next();
      PertSource ps = sourceDefs_.get(sdkey);
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
    Iterator<String> dpnkit = undo.srcDefsSubsetOrig.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = dpnkit.next();
      PertSource ps = undo.srcDefsSubsetOrig.get(key);
      PertSource psc = ps.clone();
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
    Iterator<String> dpnkit = redo.srcDefsSubsetNew.keySet().iterator();
    while (dpnkit.hasNext()) {
      String key = dpnkit.next();
      PertSource ps = redo.srcDefsSubsetNew.get(key);
      PertSource psc = ps.clone();
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
  
  public PertDataChange dropTargetAnnotations(String annotID, Set<String> keysToPrune) {
    PertDataChange retval = null;
    Iterator<String> kpit = keysToPrune.iterator();
    while (kpit.hasNext()) {
      String dpkey = kpit.next();
      List<String> noteList = targetGeneNotes_.get(dpkey); 
      if (noteList != null) {
        if (retval == null) {
          retval = new PertDataChange(serialNumber_, PertDataChange.Mode.TARG_ANNOTS_SET);  
          retval.targetNotesSubsetOrig = new HashMap<String, List<String>>();
          retval.targetNotesSubsetNew = new HashMap<String, List<String>>();
        }
        retval.targetNotesSubsetOrig.put(dpkey, new ArrayList<String>(noteList));
        noteList.remove(annotID);
        if (noteList.isEmpty()) {
          targetGeneNotes_.remove(dpkey); 
        }
        // Empty list MUST cause deletion on redo!!!!      
        retval.targetNotesSubsetNew.put(dpkey, new ArrayList<String>(noteList));
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
  
  public PertDataChange[] deletePertSourceDefs(Set<String> keysToDrop) {
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
    Iterator<String> kit = keysToDrop.iterator();
    while (kit.hasNext()) {
      String keyID = kit.next();
      retlist.add(deleteSourceDef(keyID));
    }
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge all source names to a single one
  */
  
  public PertDataChange mergePertSourceNameRefs(Set<String> defsToMerge, String srcKey, Set<String> abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.SOURCE_DEF_SET);
    retval.srcDefsSubsetOrig = new HashMap<String, PertSource>();
    retval.srcDefsSubsetNew = new HashMap<String, PertSource>();
    Iterator<String> k2dit = defsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = k2dit.next();
      PertSource psd = sourceDefs_.get(key);
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
  
  public PertDataChange mergePertPropRefs(Set<String> defsToMerge, String typeKey) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.SOURCE_DEF_SET);
    retval.srcDefsSubsetOrig = new HashMap<String, PertSource>();
    retval.srcDefsSubsetNew = new HashMap<String, PertSource>();
    Iterator<String> k2dit = defsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = k2dit.next();
      PertSource psd = sourceDefs_.get(key);
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
  
  public long getSerialNumber() {
    return (serialNumber_);
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
      case DATA_POINT:
        dataPointUndo(undo);
        return;
      case EXPERIMENT:
        experimentUndo(undo);
        return;
      case DATA_POINT_SET:
        dataPointSetUndo(undo);
        return;  
      case EXPERIMENT_SET:
        experimentSetUndo(undo);
        return;   
      case SOURCE_DEF:
        sourceDefUndo(undo);
        return;  
      case USER_FIELD_VALS:
        userFieldValsUndo(undo);
        return;  
      case USER_FIELD_NAME:
        userFieldNameUndo(undo);
        return; 
      case REG_RESTRICT:
        regRestrictUndo(undo);
        return;
      case DATA_ANNOTS:
        dataAnnotUndo(undo);
        return;
      case TARGET_ANNOTS:
        targetAnnotUndo(undo);
        return;       
      case SOURCE_NAME:
        sourceNameUndo(undo);
        return;
      case EXP_CONTROL:
        expControlUndo(undo);
        return;
      case INVEST:
        investUndo(undo);
        return;
      case DATA_ANNOTS_SET:
        dataPointAnnotSetUndo(undo);
        return;
      case SOURCE_DEF_SET:  
        sourceDefSetUndo(undo);
        return;
      case MEASURE_PROP:
        measurePropUndo(undo);
        return;
      case EXP_COND:
        expCondUndo(undo);
        return;
      case MEAS_SCALE:
        measureScaleUndo(undo);
        return;
      case TARGET_NAME:
        targetNameUndo(undo);
        return;
      case ANNOTATION:
        annotationUndo(undo);
        return;
      case PERT_PROP:
        pertPropUndo(undo);
        return;
      case TARG_ANNOTS_SET:
        targetAnnotSetUndo(undo);
        return;
      case ANNOT_MODULE:  
        annotModuleUndo(undo); 
        return;
      case MEASURE_PROP_SET:  
        measurePropSetUndo(undo); 
        return;
      case NAME_MAPPER:
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
      case DATA_POINT:
        dataPointRedo(redo);
        return;
      case EXPERIMENT:
        experimentRedo(redo);
        return;
      case DATA_POINT_SET:
        dataPointSetRedo(redo);
        return;
      case EXPERIMENT_SET:
        experimentSetRedo(redo);
        return;
      case SOURCE_DEF:
        sourceDefRedo(redo);
        return;
      case USER_FIELD_VALS:
        userFieldValsRedo(redo);
        return;  
      case USER_FIELD_NAME:
        userFieldNameRedo(redo);
        return;
      case REG_RESTRICT:
        regRestrictRedo(redo);
        return;
      case DATA_ANNOTS:
        dataAnnotRedo(redo);
        return;
      case TARGET_ANNOTS:
        targetAnnotRedo(redo);
        return;
      case SOURCE_NAME:
        sourceNameRedo(redo);
        return;
      case EXP_CONTROL:
        expControlRedo(redo);
        return;
      case INVEST:
        investRedo(redo);
        return;
      case DATA_ANNOTS_SET:
        dataPointAnnotSetRedo(redo);
        return;
      case SOURCE_DEF_SET:  
        sourceDefSetRedo(redo);
        return;
      case MEASURE_PROP:
        measurePropRedo(redo);
        return;
      case EXP_COND:
        expCondRedo(redo);
        return;
      case MEAS_SCALE:
        measureScaleRedo(redo);
        return;
      case TARGET_NAME:
        targetNameRedo(redo);
        return;
      case ANNOTATION:
        annotationRedo(redo);
        return;
      case PERT_PROP:
        pertPropRedo(redo);
        return;
      case TARG_ANNOTS_SET:
        targetAnnotSetRedo(redo);
        return;
      case ANNOT_MODULE:  
        annotModuleRedo(redo);   
        return;
      case MEASURE_PROP_SET:  
        measurePropSetRedo(redo); 
        return;
      case NAME_MAPPER:
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
  
    Iterator<Experiment> psit = experiments_.values().iterator();
    while (psit.hasNext()) {
      Experiment prs = psit.next();
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
  ** Answers if we have perturbation data for the given node.  If sourceID is
  ** not null, we look for data for the source into the targ
  **
  */
  
  public boolean haveDataForNode(DBGenome dbGenome, String nodeID, String sourceID, PerturbationDataMaps pdm) {
    if (!haveData()) {
      return (false);
    }
    List<String> mapped = pdm.getDataEntryKeysWithDefault(dbGenome, nodeID, this);
    if ((mapped == null) || mapped.isEmpty()) {
      return (false);
    }
    
    List<String> smapped = null;
    if (sourceID != null) {
      smapped = pdm.getDataSourceKeysWithDefault(dbGenome, sourceID, this);
      if ((smapped == null) || smapped.isEmpty()) {
        return (false);
      }  
    }
     
    Iterator<String> mit = mapped.iterator();
    while (mit.hasNext()) {
      String key = mit.next();
      
      if (smapped == null) {
        PertFilterExpression pfe = new PertFilterExpression(new PertFilter(PertFilter.Cat.TARGET, PertFilter.Match.STR_EQUALS, key));
        List<PertDataPoint> pertsWithTarg = getPerturbations(pfe);
        if (!pertsWithTarg.isEmpty()) {
          return (true);
        }
      } else {
        PertFilter tfilt = new PertFilter(PertFilter.Cat.TARGET, PertFilter.Match.STR_EQUALS, key);
        Iterator<String> smit = smapped.iterator();
        while (smit.hasNext()) {
          String skey = smit.next();
          PertFilter sfilt = new PertFilter(PertFilter.Cat.SOURCE_NAME, PertFilter.Match.STR_EQUALS, skey);  
          PertFilterExpression pfe = new PertFilterExpression(PertFilterExpression.Op.AND_OP, tfilt, sfilt);
          List<PertDataPoint> pertsWithSrcAndTarg = getPerturbations(pfe);
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
  
  
  public PertDataChange[] changeName(String oldName, String newName, List<PerturbationDataMaps> pdms, 
                                     DBGenome dbGenome, ExperimentalDataSource eds) {
    
    if (eds.amUsingSharedExperimentalData()) {
      throw new IllegalStateException();
    }
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
    
    String foundTarg = getTargKeyFromName(oldName);
    if (foundTarg != null) {
      retlist.addAll(Arrays.asList(changeTargetNameManageIdents(foundTarg, newName, pdms, dbGenome)));
    }
    
    String foundSrc = getSourceKeyFromName(oldName);
    if (foundSrc != null) {
      retlist.addAll(Arrays.asList(setSourceName(foundSrc, newName, pdms, dbGenome)));
    }

    PertDataChange[] changes = retlist.toArray(new PertDataChange[retlist.size()]);
    return (changes);
  }  
  
  /***************************************************************************
  **
  ** Add or replace an experiment
  */
  
  public PertDataChange setExperiment(Experiment exp) { 
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXPERIMENT); 
    Experiment currExp = experiments_.get(exp.getID());
    retval.expOrig = (currExp == null) ? null : (Experiment)currExp.clone();
    experiments_.put(exp.getID(), exp);
    retval.expNew = exp.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }

  /***************************************************************************
  **
  ** Find batch IDs for an experiment
  */
  
  public Map<String, Set<String>> getBatchKeysForExperiment(String expKey) {
    HashMap<String, Set<String>> retval = new HashMap<String, Set<String>>();
    Iterator<String> dpit = dataPoints_.keySet().iterator();
    while (dpit.hasNext()) {
      String dpKey = dpit.next();
      PertDataPoint pdp = dataPoints_.get(dpKey);
      if (expKey.equals(pdp.getExperimentKey())) {
        String batchID = pdp.getBatchKey();
        String targetID = pdp.getTargetKey();
        Set<String> forTarg = retval.get(targetID);
        if (forTarg == null) {
          forTarg = new HashSet<String>();
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
  
  public Map<String, Map<String, List<String>>> mergeExperimentBatchCollisions(Set<String> keyIDs) {
    HashMap<String, Map<String, List<String>>> seenKeys = new HashMap<String, Map<String, List<String>>>();
    Iterator<String> dpit = dataPoints_.keySet().iterator();
    while (dpit.hasNext()) {
      String dpKey = dpit.next();
      PertDataPoint pdp = dataPoints_.get(dpKey);
      String expKey = pdp.getExperimentKey();
      if (keyIDs.contains(expKey)) {
        String batchID = pdp.getBatchKey();
        String targetID = pdp.getTargetKey();    
        Map<String, List<String>> forTarg = seenKeys.get(targetID);
        if (forTarg == null) {
          forTarg = new HashMap<String, List<String>>();
          seenKeys.put(targetID, forTarg);
        }
        List<String> vals = forTarg.get(batchID);
        if (vals == null) {
          vals = new ArrayList<String>();
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

  public SortedMap<String, SortedMap<String, SortedMap<String, BatchCollision>>> prepBatchCollisions(Map<String, Map<String, List<String>>> collisions, 
                                                                                                     String expString) {   
    boolean haveOne = false;
    TreeMap<String, SortedMap<String, SortedMap<String, BatchCollision>>> retval = new TreeMap<String, SortedMap<String, SortedMap<String, BatchCollision>>>();
    TreeMap<String, SortedMap<String, BatchCollision>> perExp = new TreeMap<String, SortedMap<String, BatchCollision>>();
    retval.put(expString, perExp);
    Iterator<String> cit = collisions.keySet().iterator();
    while (cit.hasNext()) {
      String targetID = cit.next();
      Map<String, List<String>> forTarg = collisions.get(targetID);
      TreeMap<String, BatchCollision> perTarg = new TreeMap<String, BatchCollision>();
      String targName = getTarget(targetID);
      perExp.put(targName, perTarg);
      Iterator<String> ftit = forTarg.keySet().iterator();
      while (ftit.hasNext()) {
        String batchID = ftit.next();    
        List<String> vals = forTarg.get(batchID);
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
  
  public PertDataChange[] mergeExperiments(List<String> keyIDs, String useKey, Experiment revisedExp) { 
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setExperiment(revisedExp));
      } else {
        retlist.add(deleteExperiment(keyID));
      }
    }
    return (retlist.toArray(new PertDataChange[retlist.size()]));
  }
  
  /***************************************************************************
  **
  ** Merge all experiment refs to a single one
  */
  
  public PertDataChange mergeExperimentRefs(Set<String> pointsToMerge, String expKey, Set<String> abandonKeys) {
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.DATA_POINT_SET);
    retval.dataPtsSubsetOrig = new HashMap<String, PertDataPoint>();
    retval.dataPtsSubsetNew = new HashMap<String, PertDataPoint>();
    Iterator<String> k2dit = pointsToMerge.iterator();
    while (k2dit.hasNext()) {
      String key = k2dit.next();
      PertDataPoint pdp = dataPoints_.get(key);
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.EXPERIMENT);    
    retval.expOrig = experiments_.get(psiID).clone();
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
      Experiment expu = undo.expOrig.clone();
      String repairID = expu.getID();
      experiments_.put(repairID, expu);
      labels_.addExistingLabel(repairID);
    } else {  // Change data
      Experiment expu = undo.expOrig.clone();
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
      Experiment expn = redo.expNew.clone();
      String repairID = expn.getID();
      experiments_.put(repairID, expn);
      labels_.addExistingLabel(repairID);
    } else if (redo.expNew == null) {  // Redo a delete
      String repairID = redo.expOrig.getID();
      experiments_.remove(repairID);
      labels_.removeLabel(repairID);
    } else {  // Change data
      Experiment expn = redo.expNew.clone();
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
      Iterator<String> kit = undo.expSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = kit.next();
        Experiment exp = undo.expSubsetOrig.get(key);
        Experiment expc = exp.clone();
        experiments_.put(key, expc);
        labels_.addExistingLabel(key);
      }
    } else {  // Change data
      Iterator<String> kit = undo.expSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = kit.next();
        Experiment exp = undo.expSubsetOrig.get(key);
        Experiment expc = exp.clone();
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
      Iterator<String> kit = redo.expSubsetOrig.keySet().iterator();
      while (kit.hasNext()) {
        String key = kit.next();
        experiments_.remove(key);
        labels_.removeLabel(key);
      }
    } else {  // Redo Change data
      Iterator<String> kit = redo.expSubsetNew.keySet().iterator();
      while (kit.hasNext()) {
        String key = kit.next();
        Experiment exp = redo.expSubsetNew.get(key);
        Experiment expc = exp.clone();
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
    return (experiments_.get(key));
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.SOURCE_DEF);    
    PertSource currPs = sourceDefs_.get(ps.getID());
    retval.srcDefOrig = (currPs == null) ? null : (PertSource)currPs.clone();
    sourceDefs_.put(ps.getID(), ps);
    retval.srcDefNew = ps.clone();
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Delete perturbation source def
  */
  
  public PertDataChange deleteSourceDef(String psID) { 
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.SOURCE_DEF);    
    retval.srcDefOrig = sourceDefs_.get(psID).clone();
    sourceDefs_.remove(psID);
    retval.srcDefNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Merge perturbation source defs
  */
  
  public PertDataChange[] mergePertSourceDefs(List<String> keyIDs, String useKey, PertSource revisedPsrc) { 
    ArrayList<PertDataChange> retlist = new ArrayList<PertDataChange>();
    int numIDs = keyIDs.size();
    for (int i = 0; i < numIDs; i++) {
      String keyID = keyIDs.get(i);
      if (keyID.equals(useKey)) {
        retlist.add(setSourceDef(revisedPsrc));
      } else {
        retlist.add(deleteSourceDef(keyID));
      }
    }
    return (retlist.toArray(new PertDataChange[retlist.size()]));
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
      PertSource psc = undo.srcDefOrig.clone();
      sourceDefs_.put(repairID, psc);
      labels_.addExistingLabel(repairID);
    } else {  // Change data
      String repairID = undo.srcDefOrig.getID();
      PertSource psc = undo.srcDefOrig.clone();
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
      PertSource psc = redo.srcDefNew.clone();
      sourceDefs_.put(repairID, psc);
      labels_.addExistingLabel(repairID);
    } else if (redo.srcDefNew == null) {  // Redo a delete
      String repairID = redo.srcDefOrig.getID();
      sourceDefs_.remove(repairID);
      labels_.removeLabel(repairID);
    } else {  // Change data
      String repairID = redo.srcDefNew.getID();
      PertSource psc = redo.srcDefNew.clone();
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.DATA_POINT);    
    PertDataPoint currPdp = dataPoints_.get(pdp.getID());
    retval.pdpOrig = (currPdp == null) ? null : (PertDataPoint)currPdp.clone();
    dataPoints_.put(pdp.getID(), pdp);
    retval.pdpNew = pdp.clone();
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
        ArrayList<String> notes = new ArrayList<String>(undo.dataAnnotsOrig);
        dataPointNotes_.put(repairID, notes);
      }
      if (undo.dataRegResOrig != null) {
        RegionRestrict regReg = undo.dataRegResOrig.clone();
        regionRestrictions_.put(repairID, regReg);
      }
      if (undo.userDataOrig != null) {
        ArrayList<String> values = new ArrayList<String>(undo.userDataOrig);
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
      PertDataPoint pdpn = redo.pdpNew.clone();
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
      PertDataPoint pdpn = redo.pdpNew.clone();
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
    PertDataChange retval = new PertDataChange(serialNumber_, PertDataChange.Mode.DATA_POINT);    
    retval.pdpOrig = dataPoints_.get(pdpID).clone();
    dataPoints_.remove(pdpID);
    labels_.removeLabel(pdpID);
    
    //
    // Remove annotations, if present:
    //
    
    List<String> notes = dataPointNotes_.get(pdpID);  // may be null
    if (notes != null) {
      retval.dataAnnotsOrig = new ArrayList<String>(notes);
      dataPointNotes_.remove(pdpID);
    }
    
    //
    // Remove region restrict, if present:
    //
    
    RegionRestrict regReg = regionRestrictions_.get(pdpID);  // may be null
    if (regReg != null) {
      retval.dataRegResOrig = regReg.clone();
      regionRestrictions_.remove(pdpID);
    } 
    
    //
    // Remove user data:
    //
    
    List<String> values = userData_.get(pdpID);  // may be null
    if (values != null) {
      retval.userDataOrig = new ArrayList<String>(values);
      userData_.remove(pdpID);
    }

    retval.pdpNew = null;
    retval.serialNumberNew = ++serialNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Drop the perturbation data matching the filter
  */
   
  public PertDataChange dropPertDataForFilter(PertFilterExpression pfe) {
    List<PertDataPoint> dropPoints = getPerturbations(pfe);
    HashSet<String> keysToDrop = new HashSet<String>();
    int numNew = dropPoints.size();
    for (int i = 0; i < numNew; i++) {
      PertDataPoint pdp = dropPoints.get(i);
      keysToDrop.add(pdp.getID());
    }   
    return (deleteDataPoints(keysToDrop));
  }
     
  /***************************************************************************
  **
  ** Gets network elements for a root network
  */
  
  public Set<QpcrSourceSet> getNetworkElements() {
    HashSet<QpcrSourceSet> retval = new HashSet<QpcrSourceSet>();
    List<PertDataPoint> sigPerts = getSignificantPerturbations();
    int numSig = sigPerts.size();
    for (int i = 0; i < numSig; i++) {
      PertDataPoint pdp = sigPerts.get(i);
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
  ** Set the map from a node to a List of targets, used in legacy
  ** imports only!
  */
  
  public void importLegacyEntryMapEntry(String key, List<String> entries, PerturbationDataMaps pdm) {
    buildInvertTrgNameCache();
    pdm.importLegacyEntryMapEntry(key, entries, invertTargNameCache_);
    return;
  }
  
  /***************************************************************************
  **
  ** Set the map from a node to a List of sources, used in legacy
  ** imports only!
  */
  
  public void importLegacySourceMapEntry(String key, List<String> entries, PerturbationDataMaps pdm) {
    buildInvertSrcNameCache();
    pdm.importLegacySourceMapEntry(key, entries, invertSrcNameCache_);
    return;
  }

  /***************************************************************************
  **
  ** Get single-source perturbation source IDs for the given targetID
  */
  
  public Set<String> getPerturbationSources(DBGenome dbGenome, String targetID, PerturbationDataMaps pdm) {    
    
    //
    // Get the genes that match the target ids.  Go through the
    // perturbation sources and find the perturbations.
    //
    HashSet<String> retval = new HashSet<String>();
  
    List<PertDataPoint> stPerts = getSrcTargPerts(dbGenome, null, targetID, pdm);
    int numSt = stPerts.size();
    if (numSt == 0) {
      return (retval);
    }
    
    buildInvertSrcNameCache(); 
    for (int i = 0; i < numSt; i++) {
      PertDataPoint pdp = stPerts.get(i);
      String sskey = pdp.getSingleSourceKey(this);
      if (sskey != null) {
        retval.addAll(pdm.getDataSourceKeyInverse(dbGenome, sskey, invertSrcNameCache_));
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the Ids of source names for proxies
  */
  
  public Set<String> getProxiedSources() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<PertSource> sdit = sourceDefs_.values().iterator();
    while (sdit.hasNext()) {
      PertSource chk = sdit.next();
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
  
  public Map<String, Set<String>> getMappedProxiedSources() {
    HashMap<String, Set<String>> retval = new HashMap<String, Set<String>>();
    Iterator<PertSource> sdit = sourceDefs_.values().iterator();
    while (sdit.hasNext()) {
      PertSource chk = sdit.next();
      if (chk.isAProxy()) {
        String srcKey = chk.getSourceNameKey();
        Set<String> allProx = retval.get(srcKey);
        if (allProx == null) {
          allProx = new HashSet<String>();
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
    HashSet<String> retval = new HashSet<String>();
    Iterator<String> sdit = sourceDefs_.keySet().iterator();
    while (sdit.hasNext()) {
      String chkID = sdit.next();
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
  
  public List<PertDataPoint> getSignificantPerturbations() {
    if ((sigPertCache_ == null) || (sigPertCacheVersionSN_ != serialNumber_)) {
      PertFilter pertFilter = new PertFilter(PertFilter.Cat.VALUE, PertFilter.Match.ABOVE_THRESH, null);
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
  
  public PertFilterExpression getFilterExpression(DBGenome dbGenome, String sourceID, String targetID, boolean allowProxy, PerturbationDataMaps pdm) {
    List<String> skeys = (sourceID == null) ? null : pdm.getDataSourceKeysWithDefault(dbGenome, sourceID, this);
    List<String> tkeys = pdm.getDataEntryKeysWithDefault(dbGenome, targetID, this);
    return (getFilterExpression((sourceID == null), skeys, tkeys, allowProxy));
  }
  
  /***************************************************************************
  **
  ** Get a filter expression for the given src and target
  */
  
  public PertFilterExpression getFilterExpression(boolean skipSource, List<String> skeys, List<String> tkeys, boolean allowProxy) {
    PertFilter.Cat srcFilterCat = (allowProxy) ? PertFilter.Cat.SOURCE_OR_PROXY_NAME : PertFilter.Cat.SOURCE_NAME;
       
    PertFilterExpression srcExp = null;
    if ((skeys == null) || skeys.isEmpty()) {
      PertFilterExpression.Op useOp = (skipSource) ? PertFilterExpression.Op.ALWAYS_OP : PertFilterExpression.Op.NEVER_OP;
      srcExp = new PertFilterExpression(useOp);
      if (useOp == PertFilterExpression.Op.NEVER_OP) {
        return (srcExp);
      }
    } else { 
      int numS = skeys.size();
      for (int i = 0; i < numS; i++) {
        String sKey = skeys.get(i);
        PertFilter srcFilter = new PertFilter(srcFilterCat, PertFilter.Match.STR_EQUALS, sKey);
        if (srcExp == null) {
          srcExp = new PertFilterExpression(srcFilter);
        } else {
          srcExp = new PertFilterExpression(PertFilterExpression.Op.OR_OP, srcExp, srcFilter);
        }
      }
    }
    
    PertFilterExpression trgExp = null;
    if ((tkeys == null) || tkeys.isEmpty()) {
      trgExp = new PertFilterExpression(PertFilterExpression.Op.NEVER_OP);
      return (trgExp);
    } else {    
      int numT = tkeys.size();
      for (int i = 0; i < numT; i++) {
        String tKey = tkeys.get(i);
        PertFilter trgFilter = new PertFilter(PertFilter.Cat.TARGET, PertFilter.Match.STR_EQUALS, tKey);
        if (trgExp == null) {
          trgExp = new PertFilterExpression(trgFilter);
        } else {
          trgExp = new PertFilterExpression(PertFilterExpression.Op.OR_OP, trgExp, trgFilter);
        }
      }
    }
      
    PertFilterExpression exp = (srcExp == null) ? trgExp : new PertFilterExpression(PertFilterExpression.Op.AND_OP, trgExp, srcExp);
    return (exp);
  }
  
  /***************************************************************************
  **
  ** Get the perturbations between the source and target
  */
  
  public List<PertDataPoint> getSrcTargPerts(DBGenome dbGenome, String sourceID, String targetID, PerturbationDataMaps pdm) {
    PertFilterExpression fex = getFilterExpression(dbGenome, sourceID, targetID, false, pdm);
    return (getPerturbations(fex));
  } 
  
  /***************************************************************************
  **
  ** Answer if we have significant data matching the target, with relaxed name matching criteria:
  */
  
  public boolean haveDataRelaxedMatch(String targetName) {
    List<PertDataPoint> sigPerts = getSignificantPerturbations();
    int numSig = sigPerts.size();
    for (int i = 0; i < numSig; i++) {
      PertDataPoint pdp = sigPerts.get(i);
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
  
  public double getPertAverageValue(DBGenome dbGenome, String sourceID, String targetID, boolean avgSigOnly, PerturbationDataMaps pdm) { 
    List<PertDataPoint> stPerts = getSrcTargPerts(dbGenome, sourceID, targetID, pdm);
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
  
  public String getToolTip(DBGenome dbGenome, String sourceID, String targetID, 
                           String scaleKey, PerturbationDataMaps pdm, TimeAxisDefinition tad) {
    //
    // Get the pert data:
    //
    
    List<PertDataPoint> stPerts = getSrcTargPerts(dbGenome, sourceID, targetID, pdm);
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
      
    HashMap<SrcTarg, SortedMap<MinMax, Map<String, List<String>>>> tipData = new HashMap<SrcTarg, SortedMap<MinMax, Map<String, List<String>>>>();
    for (int i = 0; i < numstp; i++) {
      PertDataPoint pdp = stPerts.get(i);
      
      String display = pdp.getScaledDisplayValueOldStyle(scaleKey, this, false);
      String batchKey = pdp.getDecoratedBatchKey(false);
      MinMax time = pdp.getTimeRange(this);
   
      SrcTarg stKey = new SrcTarg(pdp.getPertDisplayString(this, PertSources.NO_FOOTS), pdp.getTargetKey());
      SortedMap<MinMax, Map<String, List<String>>> dataForST = tipData.get(stKey);
      if (dataForST == null) {
        dataForST = new TreeMap<MinMax, Map<String, List<String>>>();
        tipData.put(stKey, dataForST);
      }
      Map<String, List<String>> batchesForTime = dataForST.get(time);
      if (batchesForTime == null) {
        batchesForTime = new HashMap<String, List<String>>();
        dataForST.put(time, batchesForTime);
      }
      List<String> dataForBatch = batchesForTime.get(batchKey);
      if (dataForBatch == null) {
        dataForBatch = new ArrayList<String>();
        batchesForTime.put(batchKey, dataForBatch);
      }
      dataForBatch.add(display);
    }
    
    //
    // Merge time points into time ranges, if needed:
    //
   
    Iterator<SrcTarg> tdit = tipData.keySet().iterator();
    while (tdit.hasNext()) {  
      SrcTarg stKey = tdit.next();   
      SortedMap<MinMax, Map<String, List<String>>> dataForST = tipData.get(stKey);
      Iterator<MinMax> dfstit = dataForST.keySet().iterator();
      TreeMap<MinMax, Map<String, List<String>>> reducedForST = new TreeMap<MinMax, Map<String, List<String>>>();
      while (dfstit.hasNext()) {  
        MinMax time = dfstit.next();
        if (time.min != time.max) {
          reducedForST.put(time, dataForST.get(time));
        }
      }
      dfstit = dataForST.keySet().iterator();
      while (dfstit.hasNext()) {  
        MinMax time = dfstit.next();
        Map<String, List<String>> batchesForTimeSrc = dataForST.get(time);
        if (time.min == time.max) {
          boolean transferred = false;
          Iterator<MinMax> redit = reducedForST.keySet().iterator();
          while (redit.hasNext()) {
            MinMax candidate = redit.next();
            if ((candidate.min != candidate.max) && candidate.contained(time.min)) {
              Map<String, List<String>> batchesForTimeTarg = reducedForST.get(candidate);              
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
      SrcTarg stKey = tdit.next();
      buf.append(stKey.srcID);
      buf.append(": ");
      buf.append(getTarget(stKey.targID));
      buf.append("<br>");        
      SortedMap<MinMax, Map<String, List<String>>> dataForST = tipData.get(stKey);
      Iterator<MinMax> dfstit = dataForST.keySet().iterator();
      while (dfstit.hasNext()) {  
        MinMax time = dfstit.next();
        buf.append("&nbsp;&nbsp;"); 
        buf.append(Experiment.getTimeDisplayString(tad, time, true, true));   
        buf.append(": ");
        Map<String, List<String>> batchesForTime = dataForST.get(time);
        Iterator<String> bftit = batchesForTime.keySet().iterator();
        while (bftit.hasNext()) {  
          String batchKey = bftit.next();
          List<String> dataForBatch = batchesForTime.get(batchKey);
          Iterator<String> dfbit = dataForBatch.iterator();
          while (dfbit.hasNext()) {  
            String dataStr = dfbit.next();
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
    
    if (haveData()) {
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
      
      TreeSet<String> tKeys = new TreeSet<String>(targets_.keySet());
      if (!tKeys.isEmpty()) {
        ind.indent();
        out.println("<pertTargs>");
        ind.up();
        Iterator<String> tkit = tKeys.iterator();
        while (tkit.hasNext()) {
          String tkey = tkit.next();
          String targ = targets_.get(tkey);
          ind.indent();
          out.print("<pertTarg id=\"");
          out.print(tkey);
          out.print("\" name=\"");
          out.print(CharacterEntityMapper.mapEntities(targ, false));
          List<String> notes = getFootnotesForTarget(tkey);
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
      
      TreeSet<String> snKeys = new TreeSet<String>(sourceNames_.keySet());
      if (!snKeys.isEmpty()) {
        ind.indent();
        out.println("<pertSrcNames>");
        ind.up();
        Iterator<String> tkit = snKeys.iterator();
        while (tkit.hasNext()) {
          String snkey = tkit.next();
          String srcName = sourceNames_.get(snkey);
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
      
      TreeSet<String> iKeys = new TreeSet<String>(investigators_.keySet());
      if (!iKeys.isEmpty()) {
        ind.indent();
        out.println("<pertInvests>");
        ind.up();
        Iterator<String> ikit = iKeys.iterator();
        while (ikit.hasNext()) {
          String ikey = ikit.next();
          String invest = investigators_.get(ikey);
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
      
      TreeSet<String> sdKeys = new TreeSet<String>(sourceDefs_.keySet());
      if (!sdKeys.isEmpty()) {
        ind.indent();
        out.println("<pertSourceDefs>");
        ind.up();
        Iterator<String> sdkit = sdKeys.iterator();
        while (sdkit.hasNext()) {
          String sdkey = sdkit.next();
          PertSource psd = sourceDefs_.get(sdkey);
          psd.writeXML(out, ind);
        }   
        ind.down().indent(); 
        out.println("</pertSourceDefs>");
      }
      
      //
      // Experiments:
      //  
      
      TreeSet<String> psKeys = new TreeSet<String>(experiments_.keySet());
      if (!psKeys.isEmpty()) {
        ind.indent();
        out.println("<experiments>");
        ind.up();
        Iterator<String> pskit = psKeys.iterator();
        while (pskit.hasNext()) {
          String pskey = pskit.next();
          Experiment psi = experiments_.get(pskey);
          psi.writeXML(out, ind);
        }   
        ind.down().indent(); 
        out.println("</experiments>");
      }
      
      //
      // Region restrictions:
      //  
      
      TreeSet<String> rrKeys = new TreeSet<String>(regionRestrictions_.keySet());
      if (!rrKeys.isEmpty()) {
        ind.indent();
        out.println("<regRestricts>");
        ind.up();
        Iterator<String> rrkit = rrKeys.iterator();
        while (rrkit.hasNext()) {
          String rrkey = rrkit.next();
          RegionRestrict rr = regionRestrictions_.get(rrkey);
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
        Iterator<String> ufit = userFields_.iterator();
        while (ufit.hasNext()) {
          String fieldName = ufit.next();
          ind.indent();
          out.print("<userField name=\"");
          out.print(CharacterEntityMapper.mapEntities(fieldName, false));
          out.println("\"/>");
        }   
        ind.down().indent(); 
        out.println("</userFields>");
   
        TreeSet<String> edKeys = new TreeSet<String>(userData_.keySet());
        if (!edKeys.isEmpty()) {
          ind.indent();
          out.println("<userDataVals>");
          ind.up();
          Iterator<String> edkit = edKeys.iterator();
          while (edkit.hasNext()) {
            String udfkey = edkit.next();
            List<String> udf = userData_.get(udfkey);
            writeXMLForUserData(out, ind, udf, udfkey);
          }   
          ind.down().indent(); 
          out.println("</userDataVals>");
        }
      }
      
      TreeSet<String> dpKeys = new TreeSet<String>(dataPoints_.keySet());
      if (!dpKeys.isEmpty()) {
        ind.indent();
        out.println("<dataPoints>");
        ind.up();
        Iterator<String> dpkit = dpKeys.iterator();
        while (dpkit.hasNext()) {
          String dpkey = dpkit.next();
          PertDataPoint pdp = dataPoints_.get(dpkey);
          pdp.writeXML(out, ind, this);
        }
        ind.down().indent(); 
        out.println("</dataPoints>");
      }
    }
    //
    // Starting V8, this goes with pert data, not with other display options:
    //
    
    pdo_.writeXML(out, ind);
    
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
  
  public void writeXMLForUserData(PrintWriter out, Indenter ind, List<String> udf, String udfkey) {
    ind.indent();
    out.print("<userData key=\"");
    out.print(udfkey);
    out.print("\"");
    int numUDF = udf.size();
    for (int i = 0; i < numUDF; i++) {
      String datVal = udf.get(i);
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
  
  public static String getTimeDisplay(TimeAxisDefinition tad, Integer timeObj, boolean showUnits, boolean abbreviate) {
    if (timeObj == null) {
      return (null);
    }

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
  
  /***************************************************************************
  **
  ** Build up a full network of all interactions.  The links candidates returned
  ** have QPCR id tags in them, not genome ID tags.
  */
  
  public Set<LinkCandidate> buildFullInteractionNetwork(CollapseMode collapseMode) {

    HashMap<SrcTarg, Integer> resultMap = new HashMap<SrcTarg, Integer>();
    Map<CollapseMode, Map<Integer, Integer>> collapseMaps = buildCollapseMaps();
 
    List<PertDataPoint> sigPerts = getSignificantPerturbations();
    int numSig = sigPerts.size();
    for (int i = 0; i < numSig; i++) {
      PertDataPoint pdp = sigPerts.get(i);
      int linkSign = pdp.getLink(this);
      if (linkSign == PertProperties.NO_LINK) {
        continue;
      }
      int stSign = mapDataSignToSignTag(linkSign);
      Experiment psi = pdp.getExperiment(this);
      Iterator<String> sit = psi.getSources().getSources();
      while (sit.hasNext()) {
        String srcID = sit.next();    
        PertSource src = getSourceDef(srcID);
        SrcTarg stk = new SrcTarg(src.getSourceNameKey(), pdp.getTargetKey());
        Integer vals = resultMap.get(stk);
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
    Iterator<SrcTarg> rmit = resultMap.keySet().iterator();
    while (rmit.hasNext()) {
      SrcTarg stk = rmit.next();
      Integer vals = resultMap.get(stk);
      collapseToCandidates(collapseMode, collapseMaps, stk, vals, retval);      
    }
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
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
  
  private void collapseToCandidates(CollapseMode collapseMode, Map<CollapseMode, Map<Integer, Integer>> maps, 
                                    SrcTarg stk, Integer vals, Set<LinkCandidate> retval) {    
    Map<Integer, Integer> collapse = maps.get(collapseMode);

    int result = collapse.get(vals).intValue();
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
  
  private SortedMap<CollapseMode, Map<Integer, Integer>> buildCollapseMaps() {  

    HashMap<Integer, Integer> noCollapse = new HashMap<Integer, Integer>();
    noCollapse.put(new Integer(NO_LINK_), new Integer(NO_LINK_));  
    noCollapse.put(new Integer(PLUS_), new Integer(PLUS_));
    noCollapse.put(new Integer(MINUS_), new Integer(MINUS_));
    noCollapse.put(new Integer(UNDEFINED_), new Integer(UNDEFINED_));
    noCollapse.put(new Integer(P_AND_M_), new Integer(P_AND_M_));
    noCollapse.put(new Integer(P_AND_U_), new Integer(P_AND_U_));
    noCollapse.put(new Integer(M_AND_U_), new Integer(M_AND_U_));
    noCollapse.put(new Integer(ALL_VALS_), new Integer(ALL_VALS_));  

    HashMap<Integer, Integer> distinctPM = new HashMap<Integer, Integer>();
    distinctPM.put(new Integer(NO_LINK_), new Integer(NO_LINK_));  
    distinctPM.put(new Integer(PLUS_), new Integer(PLUS_));
    distinctPM.put(new Integer(MINUS_), new Integer(MINUS_));
    distinctPM.put(new Integer(UNDEFINED_), new Integer(UNDEFINED_));
    distinctPM.put(new Integer(P_AND_M_), new Integer(P_AND_M_DISTINCT_PM_));
    distinctPM.put(new Integer(P_AND_U_), new Integer(P_AND_U_DISTINCT_PM_));
    distinctPM.put(new Integer(M_AND_U_), new Integer(M_AND_U_DISTINCT_PM_));
    distinctPM.put(new Integer(ALL_VALS_), new Integer(ALL_VALS_DISTINCT_PM_));  

    HashMap<Integer, Integer> dropSign = new HashMap<Integer, Integer>();
    dropSign.put(new Integer(NO_LINK_), new Integer(NO_LINK_));  
    dropSign.put(new Integer(PLUS_), new Integer(PLUS_));
    dropSign.put(new Integer(MINUS_), new Integer(MINUS_));
    dropSign.put(new Integer(UNDEFINED_), new Integer(UNDEFINED_));
    dropSign.put(new Integer(P_AND_M_), new Integer(P_AND_M_FULL_COLLAPSE_DROP_SIGN_));
    dropSign.put(new Integer(P_AND_U_), new Integer(P_AND_U_FULL_COLLAPSE_DROP_SIGN_));
    dropSign.put(new Integer(M_AND_U_), new Integer(M_AND_U_FULL_COLLAPSE_DROP_SIGN_));
    dropSign.put(new Integer(ALL_VALS_), new Integer(ALL_VALS_FULL_COLLAPSE_DROP_SIGN_));  

    HashMap<Integer, Integer> keepSign = new HashMap<Integer, Integer>();
    keepSign.put(new Integer(NO_LINK_), new Integer(NO_LINK_));  
    keepSign.put(new Integer(PLUS_), new Integer(PLUS_));
    keepSign.put(new Integer(MINUS_), new Integer(MINUS_));
    keepSign.put(new Integer(UNDEFINED_), new Integer(UNDEFINED_));
    keepSign.put(new Integer(P_AND_M_), new Integer(P_AND_M_FULL_COLLAPSE_KEEP_SIGN_));
    keepSign.put(new Integer(P_AND_U_), new Integer(P_AND_U_FULL_COLLAPSE_KEEP_SIGN_));
    keepSign.put(new Integer(M_AND_U_), new Integer(M_AND_U_FULL_COLLAPSE_KEEP_SIGN_));
    keepSign.put(new Integer(ALL_VALS_), new Integer(ALL_VALS_FULL_COLLAPSE_KEEP_SIGN_));

    SortedMap<CollapseMode, Map<Integer, Integer>> retval = new TreeMap<CollapseMode, Map<Integer, Integer>>();
    retval.put(CollapseMode.NO_COLLAPSE, noCollapse);
    retval.put(CollapseMode.DISTINCT_PLUS_MINUS, distinctPM);
    retval.put(CollapseMode.FULL_COLLAPSE_DROP_SIGN, dropSign);  
    retval.put(CollapseMode.FULL_COLLAPSE_KEEP_SIGN, keepSign);

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
    
    @Override
    public String toString() {
      return (getSourceName(srcID) + " " + getTarget(targID));
    }
    
    @Override    
    public int hashCode() {
      return (srcID.hashCode() + targID.hashCode());
    }
    
    @Override
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
    
    @Override    
    public String toString() {
      return (name);
    }
    
    @Override    
    public int hashCode() {
      return (DataUtil.normKey(name).hashCode());
    }

    //
    // Two source names are equal if normalized versions equal:
    //
    
    @Override    
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
    
    public boolean containsRegionName(Set<String> dropNames) {
      if (isNullTargetStyle_) {
        return (false);
      }
      int vfrn = valsForRegular_.size();
      for (int i = 0; i < vfrn; i++) {
        String vfr = valsForRegular_.get(i);
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
      ArrayList<String> newVals = new ArrayList<String>();
      boolean dropped = false;
      int vfrn = valsForRegular_.size();
      for (int i = 0; i < vfrn; i++) {
        String vfr = valsForRegular_.get(i);
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
      ArrayList<String> newVals = new ArrayList<String>();
      boolean changed = false;
      int vfrn = valsForRegular_.size();
      for (int i = 0; i < vfrn; i++) {
        String vfr = valsForRegular_.get(i);
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
        Iterator<String> rrit = getRegions();
        while (rrit.hasNext()) {
          String reg = rrit.next();
          buf.append(reg);
          if (rrit.hasNext()) {
            buf.append(", ");
          }
        }
        retval = buf.toString();
      }
      return (retval);
    }

    @Override
    public RegionRestrict clone() {
      try {
        RegionRestrict newVal = (RegionRestrict)super.clone();
        if (this.valsForRegular_ != null) {
          // Strings in list don't need cloning:
          newVal.valsForRegular_ = new ArrayList<String>(this.valsForRegular_);
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
          String val = valsForRegular_.get(i);
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
    private boolean isForMeta_;
    private DataAccessContext dacx_;
    private Experiment.PertSourcesInfoWorker psiw_;
    private NameMapper.NameMapperWorker legacyNmw_;
    private MyLegacyNameMapperGlue legacyNmwGlue_;
    private PertDisplayOptions.PertDisplayOptionsWorker pdow_;
    
    public PertDataWorker(boolean mapsAreIllegal, boolean serialNumberIsIllegal, boolean isForMeta) {
      super(new FactoryWhiteboard());
      isForMeta_ = isForMeta;
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
     
      pdow_ = new PertDisplayOptions.PertDisplayOptionsWorker(whiteboard);
      installWorker(pdow_, new MyDisplayOpsGlue());
      
      psiw_ = new Experiment.PertSourcesInfoWorker(whiteboard);
      installWorker(psiw_, new MySourceGlue());
      
      installWorker(new PertDataPoint.PertDataPointWorker(whiteboard), new MyDataGlue()); 
      
      //
      // Older I/O has perturbation map data embedded in the Perturbation data:
      //
      legacyNmw_ = new NameMapper.NameMapperWorker(whiteboard);
      legacyNmwGlue_ =  new MyLegacyNameMapperGlue();
      
      installWorker(legacyNmw_, legacyNmwGlue_);
      
      installWorker(new RegRestrictWorker(whiteboard), new MyRegResGlue());
      installWorker(new UserDataWorker(whiteboard), new MyUserDataGlue());
      installWorker(new UserFieldWorker(whiteboard), new MyUserFieldGlue());      
    }
   

   /***************************************************************************
   **
   ** Set the current context
   */
  
    public void setContext(TabPinnedDynamicDataAccessContext dacx) {
      dacx_ = dacx;
      legacyNmwGlue_.installContext(dacx);
      return;
    }
 
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("perturbationData")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.pertData = buildFromXML(elemName, attrs);
        retval = board.pertData;
        if (board.pertData != null) {
          // Here is where we install legacy display options that now attach to each
          // perturbation display set:
          PertDisplayOptions pdo = dacx_.getDisplayOptsSource().getDisplayOptions().getLegacyPDO();
          if (pdo != null) {
            board.pertData.setLegacyPertDisplayOptions(pdo);
          }        
          if (isForMeta_) {
            dacx_.getMetabase().setSharedPertData(board.pertData);
          } else {
            dacx_.getLocalDataCopyTarget().installLocalPertData(board.pertData);
          }
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
          return (new PerturbationData(sNum));
        } catch (NumberFormatException nfex) {
          throw new IOException();
        }
      } else {
        return (new PerturbationData());
      }
    }
  }
  
  public static class MyDisplayOpsGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.pertData.setPertDisplayOptionsForIO(board.pertDisplayOptions);
      return (null);
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
  
  public static class MyLegacyNameMapperGlue implements GlueStick {
      
    private TabPinnedDynamicDataAccessContext tpdacx_;
    
    public void installContext(TabPinnedDynamicDataAccessContext dacx) {
      tpdacx_ = dacx;
      return;
    }

    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      
      //
      // Gotta get legacy map out of pert data and into database.
      // Get the perturbation maps. If none there, build it, install it, and add
      // the new data map
      //
      
      DataMapSource dms = tpdacx_.getDataMapSrc();
      PerturbationDataMaps pdm = dms.getPerturbationDataMaps();
      if (pdm == null) {
        pdm = new PerturbationDataMaps();
        dms.setPerturbationDataMaps(pdm);
      }  
      pdm.installNameMap(board.currPertDataMap);

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
        retval = new RegionRestrict(new ArrayList<String>());
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
    public Map<String, String> indexToVal;
    
    public AugUserData(String key, Map<String, String> indexToVal) {
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
      HashMap<String, String> indexToVal = new HashMap<String, String>();
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
    
    @Override
    public SerialNumberSet clone() {
      try {
        return ((SerialNumberSet)super.clone());
      } catch (CloneNotSupportedException ex) {
        throw new IllegalStateException();     
      }
    }
    
    @Override
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
    
    @Override
    public int hashCode() {
      return ((int)(pDataSN + targMapSN + srcMapSN));
    }
        
    public String getDisplayList() {
      ArrayList<String> currNums = new ArrayList<String>();
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

  public static class SourceInfo implements Comparable<SourceInfo> {
    
    public String source;
    public int sign;
        
    public SourceInfo(String source, int sign) {
      this.source = source;
      this.sign = sign;
    }
    
    public int compareTo(SourceInfo other) {
      int srcCompare = source.compareTo(other.source);
      if (srcCompare != 0) {
        return (srcCompare);
      }
      return (this.sign - other.sign);
    }
    
    @Override
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
