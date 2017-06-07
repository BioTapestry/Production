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

package org.systemsbiology.biotapestry.timeCourse;

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** This holds time course data for a target gene
*/

public class TimeCourseGene implements Cloneable, TimeCourseTableDrawer.Client {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int USE_BASE_CONFIDENCE = -1;
  public static final int NORMAL_CONFIDENCE   = 0;
  public static final int INTERPOLATED        = 1;
  public static final int INFERRED            = 2;
  public static final int QUESTIONABLE        = 3;
  public static final int ASSUMPTION          = 4;  
  public static final int NUM_CONFIDENCE      = 5; 
  public static final int LAST_WAS_STANDARD = 0;
  public static final int LAST_WAS_VARIABLE = 1;
  public static final int LAST_HAD_STRATEGY = 2;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private static final int NOT_AN_EDGE_         = 0;
  private static final int START_EDGE_          = 1;
  private static final int END_EDGE_            = 2;
  private static final int START_AND_END_EDGE_  = 3;
  private static final int REGION_DISAPPEARING_ = 4;
  private static final int SINGLE_BLOCK_REGION_ = 5;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String name_;
  private ArrayList<ExpressionEntry> data_;
  private HashMap<String, List<ExpressionEntry>> simData_;
  private TreeMap<PertSources, PerturbedTimeCourseGene> pertGenes_;
  private boolean hasTimeCourse_;
  private String timeCourseNote_;  
  private int confidence_;
  private boolean archival_;
  private boolean internalOnly_;
  private boolean isTemplate_;
  private DataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public TimeCourseGene(TimeCourseGene other) {
    this(other, true);
  }  

  /***************************************************************************
  **
  ** Constructor
  */

  public TimeCourseGene(TimeCourseGene other, boolean doData) {
    this.dacx_ = other.dacx_;
    this.name_ = other.name_;
    this.hasTimeCourse_ = other.hasTimeCourse_;
    this.timeCourseNote_ = other.timeCourseNote_;  
    this.confidence_ = other.confidence_;
    this.archival_ = other.archival_;
    this.internalOnly_ = other.internalOnly_;
    this.isTemplate_ = other.isTemplate_;
    this.data_ = new ArrayList<ExpressionEntry>();
    this.pertGenes_ = new TreeMap<PertSources, PerturbedTimeCourseGene>();
    this.simData_ = new HashMap<String, List<ExpressionEntry>>();
    if (doData) {
      int size = other.data_.size();
      for (int i = 0; i < size; i++) {
        ExpressionEntry exp = other.data_.get(i);
        this.data_.add(exp.clone());
      }
    }
    Iterator<String> sit = other.simData_.keySet().iterator();
    while (sit.hasNext()) {
      String sKey = sit.next();
      List<ExpressionEntry> see = other.simData_.get(sKey);
      List<ExpressionEntry> tsee = new ArrayList<ExpressionEntry>();
      this.simData_.put(sKey, tsee);
      if (doData) {
        int ss = see.size();
        for (int i = 0; i < ss; i++) {
          ExpressionEntry exp = see.get(i);
          tsee.add(exp.clone());
        }
      }
    }  
    Iterator<PertSources> oit = other.pertGenes_.keySet().iterator();
    while (oit.hasNext()) {
      PertSources ops = oit.next();
      PerturbedTimeCourseGene ptcg = other.pertGenes_.get(ops);      
      this.pertGenes_.put(ops.clone(), new PerturbedTimeCourseGene(ptcg, doData));
    }  
  }   

  /***************************************************************************
  **
  ** Constructor
  */

  public TimeCourseGene(DataAccessContext dacx, String name, int confidence, boolean archival, boolean internalOnly) {
    dacx_ = dacx;
    name_ = name;
    confidence_ = confidence;
    archival_ = archival;
    internalOnly_ = internalOnly;
    data_ = new ArrayList<ExpressionEntry>();
    pertGenes_ = new TreeMap<PertSources, PerturbedTimeCourseGene>();
    simData_ = new HashMap<String, List<ExpressionEntry>>(); 
  }

  /***************************************************************************
  **
  ** Constructor
  */

  public TimeCourseGene(DataAccessContext dacx, String name, Iterator<GeneTemplateEntry> tempit) {
    this(dacx, name, tempit, false);
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public TimeCourseGene(DataAccessContext dacx, String name, Iterator<GeneTemplateEntry> tempit, boolean isTemplate) {
    dacx_ = dacx;
    name_ = name;
    confidence_ = TimeCourseGene.NORMAL_CONFIDENCE;
    archival_ = false;
    internalOnly_ = false;
    data_ = new ArrayList<ExpressionEntry>();
    isTemplate_ = isTemplate;
    while (tempit.hasNext()) {
      GeneTemplateEntry gte = tempit.next();
      ExpressionEntry exp = new ExpressionEntry(gte.region, gte.time, 
                                                ExpressionEntry.NO_DATA, 
                                                TimeCourseGene.USE_BASE_CONFIDENCE);
      data_.add(exp);
    }
    pertGenes_ = new TreeMap<PertSources, PerturbedTimeCourseGene>();
    simData_ = new HashMap<String, List<ExpressionEntry>>();
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public TimeCourseGene(DataAccessContext dacx, String gene, String confidence, String timeCourse, 
                        String note, String archival, String internalOnly) 
    throws IOException {

    dacx_ = dacx;
    name_ = gene;

    if (confidence == null) {
      confidence_ = TimeCourseGene.NORMAL_CONFIDENCE;
    } else {
      try {
        confidence_ = mapToConfidence(confidence.trim());
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
    }    
        
    if (timeCourse == null) {
      throw new IOException();
    } else {
      timeCourse = timeCourse.trim();
      if (timeCourse.equalsIgnoreCase("no")) {
        hasTimeCourse_ = false;
      } else if (timeCourse.equalsIgnoreCase("yes")) {
        hasTimeCourse_ = true;
      } else {
        throw new IOException();
      }
    }
 
    if (archival != null) {
      archival = archival.trim();
      if (archival.equalsIgnoreCase("no")) {
        archival_ = false;
      } else if (archival.equalsIgnoreCase("yes")) {
        archival_ = true;
      } else {
        throw new IOException();
      }
    } else {
      archival_ = false;
    }
    
    if (internalOnly != null) {
      internalOnly = internalOnly.trim();
      if (internalOnly.equalsIgnoreCase("no")) {
        internalOnly_ = false;
      } else if (internalOnly.equalsIgnoreCase("yes")) {
        internalOnly_ = true;
      } else {
        throw new IOException();
      }
    } else {
      internalOnly_ = false;
    }    

    timeCourseNote_ = note;
    data_ = new ArrayList<ExpressionEntry>();
    pertGenes_ = new TreeMap<PertSources, PerturbedTimeCourseGene>();
    simData_ = new HashMap<String, List<ExpressionEntry>>();
  }

  /***************************************************************************
  **
  ** Constructor for the template.
  */

  private TimeCourseGene(DataAccessContext dacx, boolean isTemplate) throws IOException {

    if (!isTemplate) {
      throw new IOException();
    }
    
    dacx_ = dacx;
    name_ = "___Gene-For-BT-Template__";
    data_= new ArrayList<ExpressionEntry>();
    hasTimeCourse_ = false;
    timeCourseNote_ = null;  
    confidence_ = TimeCourseGene.NORMAL_CONFIDENCE;
    archival_ = false;
    internalOnly_ = true;
    isTemplate_ = true;
    pertGenes_ = new TreeMap<PertSources, PerturbedTimeCourseGene>();
    simData_ = new HashMap<String, List<ExpressionEntry>>();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Needed for merge checks
  */

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof TimeCourseGene)) {
      return (false);
    }
    TimeCourseGene otherTCG = (TimeCourseGene)other;
  
    if (!DataUtil.stringsEqual(this.name_, otherTCG.name_)) {
      return (false);
    }
    if (!DataUtil.stringsEqual(this.timeCourseNote_, otherTCG.timeCourseNote_)) {
      return (false);
    }
       
    if (this.hasTimeCourse_ != otherTCG.hasTimeCourse_) {
      return (false);
    }
    if (this.confidence_ != otherTCG.confidence_) {
      return (false);
    }
    if (this.archival_ != otherTCG.archival_) {
      return (false);
    }
    if (this.internalOnly_ != otherTCG.internalOnly_) {
      return (false);
    }
    if (this.isTemplate_ != otherTCG.isTemplate_) {
      return (false);
    }

    if (!this.data_.equals(otherTCG.data_)) {
      return (false);
    }

    if (!this.pertGenes_.equals(otherTCG.pertGenes_)) {
      return (false);
    }
    
    if (!this.simData_.equals(otherTCG.simData_)) {
      return (false);
    }
    return (true);
  } 
 
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public TimeCourseGene clone() {
    try {
      TimeCourseGene retval = (TimeCourseGene)super.clone();
      int size = this.data_.size();
      retval.data_ = new ArrayList<ExpressionEntry>();
      for (int i = 0; i < size; i++) {
        ExpressionEntry exp = this.data_.get(i);
        retval.data_.add(exp.clone());
      }
      retval.pertGenes_ = new TreeMap<PertSources, PerturbedTimeCourseGene>();
      Iterator<PertSources> oit = this.pertGenes_.keySet().iterator();
      while (oit.hasNext()) {
        PertSources ops = oit.next();
        PerturbedTimeCourseGene ptcg = this.pertGenes_.get(ops);
        retval.pertGenes_.put(ops.clone(), ptcg.clone());
      }
      retval.simData_ = new HashMap<String, List<ExpressionEntry>>();
      Iterator<String> sit = this.simData_.keySet().iterator();
      while (sit.hasNext()) {
        String sKey = sit.next();
        List<ExpressionEntry> see = this.simData_.get(sKey);
        List<ExpressionEntry> tsee = new ArrayList<ExpressionEntry>();
        retval.simData_.put(sKey, tsee);
        int ss = see.size();
        for (int i = 0; i < ss; i++) {
          ExpressionEntry exp = see.get(i);
          tsee.add(exp.clone());
        }
      }
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
   
  /***************************************************************************
  **
  ** Return the template equivalent for the gene
  */
  
  public List<GeneTemplateEntry> toTemplate() {
    ArrayList<GeneTemplateEntry> geneTemplate = new ArrayList<GeneTemplateEntry>();
    Iterator<ExpressionEntry> eit = getExpressions();
    while (eit.hasNext()) {
      ExpressionEntry exp = eit.next();
      int time = exp.getTime();
      String region = exp.getRegion();
      GeneTemplateEntry gte = new GeneTemplateEntry(time, region);
      geneTemplate.add(gte);
    }
    return (geneTemplate);
  }
  
  /***************************************************************************
  **
  ** Answer if we are the template
  */
  
  public boolean isTemplate() {
    return (isTemplate_);
  }
  
  /***************************************************************************
  **
  ** Get the name
  */
  
  public String getName() {
    return (name_);
  }
  
  /***************************************************************************
  **
  ** Set the name
  */
  
  public void setName(String name) {
    name_ = name;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the confidence
  */
  
  public int getConfidence() {
    return (confidence_);
  }

  /***************************************************************************
  **
  ** Set the base confidence
  */
  
  public void setConfidence(int confidence) {
    confidence_ = confidence;
    return;
  }    
 
  /***************************************************************************
  **
  ** Map a confidence
  */
  
  public int mapEntryConfidence(int entryConfidence) {
    return (mapEntryConfidence(confidence_, entryConfidence));
  }

  /***************************************************************************
  **
  ** Reverse Map a confidence
  */
  
  public int reverseMapEntryConfidence(int entryConfidence) {
    return (reverseMapEntryConfidence(confidence_, entryConfidence));
  }  
   
  /***************************************************************************
  **
  ** Set time course data 
  */
  
  public void setTimeCourse(boolean hasTimeCourse) {
    hasTimeCourse_ = hasTimeCourse;
    return;
  }  
  
  /***************************************************************************
  **
  ** Answer if time course data is available
  */
  
  public boolean hasTimeCourse() {
    return (hasTimeCourse_);
  }  
  
  /***************************************************************************
  **
  ** Answer if data is only to be used for data display, not for actual calculation
  */
  
  public boolean isArchival() {
    return (archival_);
  }
  
  /***************************************************************************
  **
  ** Answer if data is only to be used for data calculation, not for display
  */
  
  public boolean isInternalOnly() {
    return (internalOnly_);
  }  

  /***************************************************************************
  **
  ** Set if data is only to be used for data calculation, not for display
  */
  
  public void setInternalOnly(boolean state) {
    internalOnly_ = state;
    return;
  }   

  /***************************************************************************
  **
  ** Set time course note
  */
  
  public void setTimeCourseNote(String timeCourseNote) {
    timeCourseNote_ = timeCourseNote;
    return;
  }
  
  /***************************************************************************
  **
  ** Get time course note
  */
  
  public String getTimeCourseNote() {
    return (timeCourseNote_);
  }
  
  /***************************************************************************
  **
  ** Add an expression entry
  */
  
  public void addExpression(ExpressionEntry expr) {
    data_.add(expr);
    return;
  }
  
  /***************************************************************************
  **
  ** Add an expression entry for a simulation
  */
  
  public void addExpressionForSim(String simKey, ExpressionEntry expr) {
    List<ExpressionEntry> see = simData_.get(simKey);
    if (see == null) {
      see = new ArrayList<ExpressionEntry>();
      simData_.put(simKey, see);
    }
    see.add(expr);
    return;
  }

  /***************************************************************************
  **
  ** Add an expression entry
  */
  
  public void addExpression(int index, ExpressionEntry expr) {
    data_.add(index, expr);
    return;
  }
  
  /***************************************************************************
  **
  ** Add an expression entry GLOBALLY, i.e. to all the perturbed versions too
  */
  
  public void addExpressionGlobally(int index, ExpressionEntry expr) {
    data_.add(index, expr);
    Iterator<PertSources> pit = pertGenes_.keySet().iterator();
    while (pit.hasNext()) {
      PertSources ops = pit.next();
      PerturbedTimeCourseGene ptcg = pertGenes_.get(ops);
      ExpressionEntry ctrl = (ptcg.usingDistinctControlExpr()) ? expr.clone() : null;
      ptcg.addExpression(index, expr.clone(), ctrl);
    }
    Iterator<String> sit = simData_.keySet().iterator();
    while (sit.hasNext()) {
      String sKey = sit.next();
      List<ExpressionEntry> see = simData_.get(sKey);
      see.add(index, expr.clone());
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the nth expression for the given simulation key
  */
  
  public ExpressionEntry getExpressionForSim(String simKey, int n) {
    List<ExpressionEntry> see = simData_.get(simKey);
    return (see.get(n));
  } 
  
  /***************************************************************************
  **
  ** Get an iterator over the expressions for the given simulation key
  */
  
  public Iterator<ExpressionEntry> getExpressionsForSim(String simKey) {
    List<ExpressionEntry> see = simData_.get(simKey);
    return (see.iterator());
  }  
  
  /***************************************************************************
  **
  ** Get a set of all sim keys
  */
  
  public Set<String> getAllSimKeys() {
    return (new HashSet<String>(simData_.keySet()));
  }

  /***************************************************************************
  **
  ** Get the nth expression
  */
  
  public ExpressionEntry getExpression(int n) {
    return (data_.get(n));
  }  
   
  /***************************************************************************
  **
  ** Get an iterator over the expressions
  */
  
  public Iterator<ExpressionEntry> getExpressions() {
    return (data_.iterator());
  }  
  
  /***************************************************************************
  **
  ** Modify existing expressions entries for template change:
  */
  
  public void mapExpressionGlobally(int i, GeneTemplateEntry newEntry, int mapVal, 
                                    boolean isSame, TimeCourseGene oldGeneVersion) {
    addExpression(i, new ExpressionEntry(oldGeneVersion.getExpression(mapVal)));
    if (!isSame) {
      updateExpression(i, newEntry);
    }
  
    Iterator<PertSources> pit = pertGenes_.keySet().iterator();
    while (pit.hasNext()) {
      PertSources ops = pit.next();
      PerturbedTimeCourseGene ptcg = pertGenes_.get(ops);
      PerturbedTimeCourseGene oldPtcg = oldGeneVersion.getPerturbedState(ops);
      ExpressionEntry ctrl = (oldPtcg.usingDistinctControlExpr()) ? 
                              new ExpressionEntry(oldPtcg.getControlExpression(mapVal)) : null;
      ptcg.addExpression(i, new ExpressionEntry(oldPtcg.getExpression(mapVal)), ctrl);
      if (!isSame) {
        ptcg.updateExpression(i, newEntry);
      }
    }
    Iterator<String> sit = simData_.keySet().iterator();
    while (sit.hasNext()) {
      String sKey = sit.next();
      List<ExpressionEntry> see = simData_.get(sKey);
      ExpressionEntry nee = new ExpressionEntry(oldGeneVersion.getExpressionForSim(sKey, mapVal));
      see.add(i, nee);
      if (!isSame) {
        nee.updateRegionAndTime(newEntry.region, newEntry.time);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Modify existing expressions entries for region name change:
  */
  
  public void updateRegionGlobally(TimeCourseGene oldGeneVersion, String oldName, String newName) {    
    Iterator<ExpressionEntry> eit = oldGeneVersion.getExpressions();
    while (eit.hasNext()) {
      ExpressionEntry ee = eit.next();
      ExpressionEntry newee = new ExpressionEntry(ee);
      if (DataUtil.keysEqual(ee.getRegion(), oldName)) {
        newee.updateRegion(newName);
      }
      addExpression(newee);
    }
    
    Iterator<PertSources> pit = pertGenes_.keySet().iterator();
    while (pit.hasNext()) {
      PertSources ops = pit.next();
      PerturbedTimeCourseGene ptcg = pertGenes_.get(ops);
      PerturbedTimeCourseGene oldPtcg = oldGeneVersion.getPerturbedState(ops);
      Iterator<ExpressionEntry> peit = oldPtcg.getExpressions();
      Iterator<ExpressionEntry> peitc = (oldPtcg.usingDistinctControlExpr()) ? oldPtcg.getControlExpressions() : null;
      while (peit.hasNext()) {
        ExpressionEntry ee = peit.next();
        ExpressionEntry newee = new ExpressionEntry(ee);
        if (DataUtil.keysEqual(ee.getRegion(), oldName)) {
          newee.updateRegion(newName);
        }
        ExpressionEntry newctrlee = null;
        if (peitc != null) {
          ee = peitc.next();
          newctrlee = new ExpressionEntry(ee);
          if (DataUtil.keysEqual(ee.getRegion(), oldName)) {
            newctrlee.updateRegion(newName);
          }
        }          
        ptcg.addExpression(newee, newctrlee);
      }
    }
    Iterator<String> sit = simData_.keySet().iterator();
    while (sit.hasNext()) {
      String sKey = sit.next();
      List<ExpressionEntry> see = simData_.get(sKey);
      Iterator<ExpressionEntry> seit = oldGeneVersion.getExpressionsForSim(sKey);
      while (seit.hasNext()) {
        ExpressionEntry ee = seit.next();
        ExpressionEntry newee = new ExpressionEntry(ee);
        if (DataUtil.keysEqual(ee.getRegion(), oldName)) {
          newee.updateRegion(newName);
        }
        see.add(newee);
      }
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Modify existing template entry
  */
  
  public void updateExpression(int i, GeneTemplateEntry newEntry) {
    ExpressionEntry ee = getExpression(i);
    ee.updateRegionAndTime(newEntry.region, newEntry.time);
    return;
  }
  
  /***************************************************************************
  **
  ** Fill in expression for the given region between the given times
  */
  
  public void fillForRegionAndTimes(String region, int timeMin, int timeMax) {
    Iterator<ExpressionEntry> eit = data_.iterator();
    while (eit.hasNext()) {
      ExpressionEntry ee = eit.next();
      if (DataUtil.keysEqual(ee.getRegion(), region)) {
        int time = ee.getTime();
        if ((time >= timeMin) && (time <= timeMax)) {
          ee.setExpression(ExpressionEntry.EXPRESSED);
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the set of source options
  */
  
  public Set<ExpressionEntry.Source> getSourceOptions() {
    return (getSourceOptions(data_));
  }

  /***************************************************************************
  **
  ** Get an iterator over the perturbed state keys (PertSources)
  */
  
  public Iterator<PertSources> getPertKeys() {
    return (pertGenes_.keySet().iterator());
  }
  
  /***************************************************************************
  **
  ** Get the specified perturbed state
  */
  
  public PerturbedTimeCourseGene getPerturbedState(PertSources key) {
    return (pertGenes_.get(key));
  }
  
  /***************************************************************************
  **
  ** Set the given perturbed state
  */
  
  public void setPerturbedState(PertSources key, PerturbedTimeCourseGene ptcg) {
    pertGenes_.put(key, ptcg);
    return;
  }
  
  /***************************************************************************
  **
  ** Clear all the perturbed states
  */
  
  public void clearPerturbedStates() {
    pertGenes_.clear();
    return;
  }
  
  /***************************************************************************
  **
  ** Drop the given perturbed state
  */
  
  public void dropPerturbedState(PertSources key) {
    pertGenes_.remove(key);
    return;
  }
  
  /***************************************************************************
  **
  ** Return the expression level for a given region and hour.
  */
  
  public int getExpressionLevelForSource(String region, int hour, ExpressionEntry.Source exprSource, VariableLevel varLev) {
    
    //
    // If the request falls outside an active region, we need
    // to return the code.
    //
    
    if (!haveARegion(region, hour)) {
      return (ExpressionEntry.NO_REGION);
    }

    //
    // Crank through all the expressions to find matching and
    // bounding entries. 
    //
    Iterator<ExpressionEntry> eit = getExpressions();
    ExpTriplet trip = findATrip(eit, region, hour);

    //
    // If we have a matching entry, figure out the expression level and return it
    //
    
    if (trip.matchingEntry != null) {
      return (calculateMatchingExpression(trip.matchingEntry, exprSource, varLev));
    }
   
    //
    // No exact match, so interpolate the results:
    //
    
    return (interpolateExpression(hour, trip.greatestLower, trip.leastHigher, exprSource, varLev));
  }
 
  /***************************************************************************
  **
  ** Return the expression level for a given region and hour and Simulation run
  */
  
  public int getExpressionLevelForSimulation(String simKey, String region, int hour, VariableLevel varLev) {
    
    //
    // If the request falls outside an active region, we need
    // to return the code.
    //
    
    if (!haveARegion(region, hour)) {
      return (ExpressionEntry.NO_REGION);
    }

    //
    // Crank through all the expressions to find matching and
    // bounding entries. 
    //
    Iterator<ExpressionEntry> eit = getExpressionsForSim(simKey);
    ExpTriplet trip = findATrip(eit, region, hour);

    //
    // If we have a matching entry, figure out the expression level and return it
    //
    
    if (trip.matchingEntry != null) {
      return (calculateMatchingExpression(trip.matchingEntry, ExpressionEntry.Source.NO_SOURCE_SPECIFIED, varLev));
    }
   
    //
    // No exact match, so interpolate the results:
    //
    
    return (interpolateExpression(hour, trip.greatestLower, trip.leastHigher, ExpressionEntry.Source.NO_SOURCE_SPECIFIED, varLev));
  }

  /***************************************************************************
  **
  ** Return the triple of bounding or matching entries
  */
  
  private ExpTriplet findATrip(Iterator<ExpressionEntry> eit, String region, int hour) {
    
    //
    // Crank through all the expressions to find matching and
    // bounding entries. 
    //
    
    ExpTriplet trip = new ExpTriplet();
    while (eit.hasNext()) {
      ExpressionEntry ee = eit.next();
      int time = ee.getTime();      
      if (!DataUtil.keysEqual(ee.getRegion(), region))  {
        continue;
      }
      if (time == hour) {
        trip.matchingEntry = ee;
      } else if (time > hour) {
        if ((trip.leastHigher == null) || (time < trip.leastHigher.getTime())) {
          trip.leastHigher = ee;
        }
      } else if (time < hour) {
        if ((trip.greatestLower == null) || (time > trip.greatestLower.getTime())) {
          trip.greatestLower = ee;
        }        
      }
    }  
    return (trip);
  }
  
  /***************************************************************************
  **
  ** Return the edge status of an expression entry.
  */
  
  private int edgeStatus(ExpressionEntry entry, ExpressionEntry.Source exprSource) {
    return (edgeStatus(entry, exprSource, getExpressions()));
  }
  
  /***************************************************************************
  **
  ** Catch the "NO_REGION" expression replies
  */
  
  private boolean haveARegion(String region, int hour) {
 
    //
    // Crank through all the expressions to find ones that
    // match the region. 
    //

    ExpressionEntry greatestLower = null;
    ExpressionEntry leastHigher = null;    
    Iterator<ExpressionEntry> eit = getExpressions();
    while (eit.hasNext()) {
      ExpressionEntry ee = eit.next();
      int time = ee.getTime();      
      if (!DataUtil.keysEqual(ee.getRegion(), region))  {
        continue;
      }
      //
      // Region matches.  Look for exact match (return), or greatest
      // lower and smallest higher.
      //
      if (time == hour) {
        return (true);
      } else if (time > hour) {
        if ((leastHigher == null) || (time < leastHigher.getTime())) {
          leastHigher = ee;
        }
      } else if (time < hour) {
        if ((greatestLower == null) || (time > greatestLower.getTime())) {
          greatestLower = ee;
        }        
      }
    }
   
    //
    // No exact match.  If we have lower and higher times, the region exists.
    //
    
    if ((greatestLower != null) && (leastHigher != null)) {
      return (true);
    } 
    
    //
    // No lowest, so we don't have a region yet:
    //
    
    if (greatestLower == null) {
      return (false);
    }
    
    //
    // If the least higher value is not there, the region may be dying out.
    // Find out if the time we are asking for falls within the granularity of
    // the data, and act accordingly.
    
    
    HashSet<Integer> rawtimes = new HashSet<Integer>();
    getInterestingTimes(rawtimes);
    TreeSet<Integer> times = new TreeSet<Integer>();
    times.addAll(rawtimes);
    Iterator<Integer> tmit = times.iterator();
    int targTime = greatestLower.getTime();
    while (tmit.hasNext()) {
      int time = tmit.next().intValue();
      if (time > targTime) {
        return (hour < time);
      }
    }
    return (false);
  }  
    
  /***************************************************************************
  **
  ** Return the expression level for a direct match.
  */
  
  private int calculateMatchingExpression(ExpressionEntry matchingEntry, ExpressionEntry.Source exprSource, VariableLevel varLev) { 

    if (matchingEntry == null) {
      throw new IllegalArgumentException();
    }

    //
    // If the matching entry is not an edge, or if it is an edge and has no
    // special edge strategy, we just return the expression value.
    //

    int matchExpr = matchingEntry.getExpressionForSource(exprSource);
    if (matchExpr == ExpressionEntry.VARIABLE) {
      varLev.level = matchingEntry.getVariableLevelForSource(exprSource);
      return (matchExpr);
    } else if (matchExpr <= ExpressionEntry.NOT_EXPRESSED) {
      return (matchExpr);
    }
    
    int startStrat = matchingEntry.getStartStrategy(exprSource);
    int endStrat = matchingEntry.getEndStrategy(exprSource);    
    int edge = edgeStatus(matchingEntry, exprSource);
    switch (edge) {
      case NOT_AN_EDGE_:
        return (matchExpr);
      case START_EDGE_:
      case SINGLE_BLOCK_REGION_:  
      case START_AND_END_EDGE_:  // Start strategy rules here        
        if (startStrat == ExpressionEntry.NO_STRATEGY_SPECIFIED) {
          return (matchExpr);
        } else if ((startStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_FAST_RAMP) ||
                   (startStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_SLOW_RAMP)) {
          return (matchExpr);
        } else if (edge == START_AND_END_EDGE_) { // Overrride bogus strategy: must be on
          return (matchExpr);
        } else {
          return (ExpressionEntry.NOT_EXPRESSED);
        }        
      case END_EDGE_:
      case REGION_DISAPPEARING_:       
        if (endStrat == ExpressionEntry.NO_STRATEGY_SPECIFIED) {
          return (matchExpr);
        } else if ((endStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_FAST_RAMP) ||
                   (endStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_SLOW_RAMP)) {
          return (matchExpr);
        } else {
          return (ExpressionEntry.NOT_EXPRESSED);
        }
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Return the interpolated expression level for a variable case.
  */
  
  private boolean interpolateVariableExpression(int hour, ExpressionEntry greatestLower, 
                                                ExpressionEntry leastHigher, 
                                                ExpressionEntry.Source exprSource, VariableLevel varLev) {
    //
    // With variable expression, there is no edge strategy!  If mixed variable
    // and fixed, convert the fixed to a variable level:
    //
    
    int lowerExpr = greatestLower.getExpressionForSource(exprSource);
    boolean lowerVar = (lowerExpr == ExpressionEntry.VARIABLE);
    boolean higherVar = false;
    if (leastHigher != null) {
      int higherExpr = leastHigher.getExpressionForSource(exprSource);
      higherVar = (higherExpr == ExpressionEntry.VARIABLE);
    }
    
    if (!lowerVar && !higherVar) {
      return (false);
    }
    
    //
    // If we have no least higher, we set the value to the lower and are done:
    //
    
    if (leastHigher == null) {
      varLev.level = greatestLower.getVariableLevelForSource(exprSource);
      return (true);
    }
    
    //
    // We have both.  Convert any fixed to variable, or get the variable level:
    //
    
    int lowerTime = greatestLower.getTime();
    VariableLevel lowerVarLev = new VariableLevel();
    if (!lowerVar) {
      convertFixedToVariable(lowerExpr, lowerVarLev);
    } else {
      lowerVarLev.level = greatestLower.getVariableLevelForSource(exprSource);
    }
    
    int higherTime = leastHigher.getTime();
    int higherExpr = leastHigher.getExpressionForSource(exprSource);
    VariableLevel higherVarLev = new VariableLevel();
    if (!higherVar) {
      convertFixedToVariable(higherExpr, higherVarLev);
    } else {
      higherVarLev.level = leastHigher.getVariableLevelForSource(exprSource);
    }
    
    //
    // Now interpolate:
    //
    
    double frac = ((double)hour - (double)lowerTime) / ((double)higherTime - (double)lowerTime);
    double diff = higherVarLev.level - lowerVarLev.level;
    varLev.level = lowerVarLev.level + (frac * diff);
    
    return (true); 
  }
  
  /***************************************************************************
  **
  ** Convert fixed values to variable ones:
  */ 
  
  private void convertFixedToVariable(int exprLevel, VariableLevel varLev) {
    switch (exprLevel) {
      case ExpressionEntry.NO_DATA:
      case ExpressionEntry.NOT_EXPRESSED:
        varLev.level = 0.0;
        break;
      case ExpressionEntry.WEAK_EXPRESSION:
        varLev.level = dacx_.getDisplayOptsSource().getDisplayOptions().getWeakExpressionLevel();
        break;
      case ExpressionEntry.EXPRESSED:        
        varLev.level = 1.0;
        break;
      case ExpressionEntry.NO_REGION:  
      case ExpressionEntry.VARIABLE:
      default:  
        throw new IllegalArgumentException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Return the interpolated expression level
  */
  
  private int interpolateExpression(int hour, ExpressionEntry greatestLower, 
                                              ExpressionEntry leastHigher, 
                                              ExpressionEntry.Source exprSource, 
                                              VariableLevel varLev) {

    //
    // We have to have a greatest lower, since otherwise we would have
    // caught this as out of region:
    //
                                                
    if (greatestLower == null) {
      throw new IllegalArgumentException();
    }
    
    //
    // With variable expression, there is no edge strategy!  If mixed variable
    // and fixed, convert the fixed to a variable level:
    //
    
    if (interpolateVariableExpression(hour, greatestLower, leastHigher, exprSource, varLev)) {
      return (ExpressionEntry.VARIABLE);
    }

    //
    // Figure out the edge status of the bounding entries.
    //
    // Note that having a start/end edge where the start says it's on and the end
    // says it's off is impossible to express correctly; with this stuff, we ignore
    // the strategies.
    
    int lowerEdge = edgeStatus(greatestLower, exprSource);
    int startStrat = greatestLower.getStartStrategy(exprSource);
    int endStrat = greatestLower.getEndStrategy(exprSource);
    int lowerExpr = greatestLower.getExpressionForSource(exprSource);
    
    switch (lowerEdge) {
      case NOT_AN_EDGE_:
        break;
      case START_EDGE_:
        if ((startStrat == ExpressionEntry.OFF_AT_BOUNDARY_WITH_SLOW_RAMP) && 
            ((hour - greatestLower.getTime()) == 1)) {
          return (ExpressionEntry.NOT_EXPRESSED);
        } else {
          return (lowerExpr);
        }
      case START_AND_END_EDGE_:
        if (ExpressionEntry.inconsistentStrategies(startStrat, endStrat)) {
          return (lowerExpr);  // Ignore strategy; maybe should throw an exception?
        } else if ((endStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_SLOW_RAMP)  && // acts like an end edge
                   ((hour - greatestLower.getTime()) == 1)) {
          return (lowerExpr);
        } else if ((endStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_FAST_RAMP) &&
                   ((hour - greatestLower.getTime()) == 1)) {
          return (ExpressionEntry.NOT_EXPRESSED);
        } else {              
          return (lowerExpr);
        }
      case SINGLE_BLOCK_REGION_:
        if ((startStrat == ExpressionEntry.OFF_AT_BOUNDARY_WITH_SLOW_RAMP) &&
            ((hour - greatestLower.getTime()) == 1)) {
          return (ExpressionEntry.NOT_EXPRESSED);
        } else if ((startStrat == ExpressionEntry.OFF_AT_BOUNDARY_WITH_FAST_RAMP) &&
                   ((hour - greatestLower.getTime()) == 1)) {
          return (lowerExpr);  
        } else {
          return (lowerExpr);
        }  
      case REGION_DISAPPEARING_:
      case END_EDGE_:
        if ((endStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_SLOW_RAMP) &&
            ((hour - greatestLower.getTime()) == 1)) {
          return (lowerExpr);
        } else if ((endStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_FAST_RAMP) &&
                   ((hour - greatestLower.getTime()) == 1)) {
          return (ExpressionEntry.NOT_EXPRESSED);          
        } else if (endStrat == ExpressionEntry.NO_STRATEGY_SPECIFIED) {  // old default case
          return (lowerExpr);          
        } else {
          return (ExpressionEntry.NOT_EXPRESSED);
        }
      default:
        throw new IllegalStateException();
    }
        
   //
   // If we got here, the lower expression was not an edge, so now we deal with
   // the upper expression.  If we have a null upper expression, then the lower
   // expression should have been nothing, otherwise we have a bad
   // state, because that would have made the lower expression an edge!
   //
    
   if (leastHigher == null) {
     if (lowerExpr > ExpressionEntry.NOT_EXPRESSED) {
       throw new IllegalStateException();
     } else {
       return (lowerExpr);
     }
   }
    
   int higherEdge = edgeStatus(leastHigher, exprSource);
   startStrat = leastHigher.getStartStrategy(exprSource);
   endStrat = leastHigher.getEndStrategy(exprSource);
   int higherExpr = leastHigher.getExpressionForSource(exprSource);
    
    switch (higherEdge) {
      case NOT_AN_EDGE_:
        return (lowerExpr);  // nobody is an edge!
      case START_EDGE_:
        if (startStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_SLOW_RAMP) {
          return (higherExpr);
        } else if ((startStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_FAST_RAMP) &&
                   ((leastHigher.getTime() - hour) == 1)) {
          return (higherExpr);
        } else {
          return (ExpressionEntry.NOT_EXPRESSED);
        }
      case START_AND_END_EDGE_:
        if (ExpressionEntry.inconsistentStrategies(startStrat, endStrat)) {
          return (lowerExpr);  // Ignore strategy; maybe should throw an exception?
        } else if (startStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_SLOW_RAMP) { // acts like a start edge
          return (higherExpr);
        } else if ((startStrat == ExpressionEntry.ON_AT_BOUNDARY_WITH_FAST_RAMP) &&
                   ((leastHigher.getTime() - hour) == 1)) {
          return (higherExpr);          
        } else {  // acts like a start edge
          return (ExpressionEntry.NOT_EXPRESSED);
        }
      case END_EDGE_:
      case REGION_DISAPPEARING_:
        if ((endStrat == ExpressionEntry.OFF_AT_BOUNDARY_WITH_SLOW_RAMP) &&
            ((leastHigher.getTime() - hour) == 1)) {            
          return (ExpressionEntry.NOT_EXPRESSED); 
        } else {
          return (higherExpr);
        }
      case SINGLE_BLOCK_REGION_: // Cannot get here!
      default:
        throw new IllegalStateException();
    }    
  }  
  
  /***************************************************************************
  **
  ** Return the confidence level for a given region and time.
  */
  
  public int getConfidence(String region, int time) {
    //
    // Crank through all the expressions to find ones that
    // match the region. 
    
    Iterator<ExpressionEntry> eit = getExpressions();
    while (eit.hasNext()) {
      ExpressionEntry ee = eit.next();
      int eTime = ee.getTime();      
      if (!DataUtil.keysEqual(ee.getRegion(), region))  {
        continue;
      }
      //
      // Region matches.  Look for exact match of time.  If we end
      // up with no match, we will throw an exception.
      //
      if (eTime == time) {
        int confidence = ee.getConfidence();
        return ((confidence == USE_BASE_CONFIDENCE) ? confidence_ : confidence);
      }
    }
    System.err.println("No confidence for " + time);
    throw new IllegalArgumentException();
  }  
  
  /***************************************************************************
  **
  ** Return the expression source for a given region and time.
  */
  
  public ExpressionEntry.Source getExprSource(String region, int time) {
    //
    // Crank through all the expressions to find ones that
    // match the region. 
    
    Iterator<ExpressionEntry> eit = getExpressions();
    while (eit.hasNext()) {
      ExpressionEntry ee = eit.next();
      int eTime = ee.getTime();      
      if (!DataUtil.keysEqual(ee.getRegion(), region))  {
        continue;
      }
      //
      // Region matches.  Look for exact match of time.  If we end
      // up with no match, we will throw an exception.
      //
      if (eTime == time) {
        return (ee.getSource());
      }
    }
    System.err.println("No source for " + time);
    throw new IllegalArgumentException();
  }
 
  /***************************************************************************
  **
  ** Get the set of "interesting times"
  **
  */
  
  public void getInterestingTimes(Set<Integer> interest) {
    Iterator<ExpressionEntry> exps = getExpressions();
    while (exps.hasNext()) {
      ExpressionEntry exp = exps.next();
      //
      // Should compare to only report when things change!  FIX ME?
      //
      int time = exp.getTime();
      interest.add(new Integer(time));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the global first expression time
  **
  */
  
  public int getGlobalFirstExpression(ExpressionEntry.Source channel) {
    int retval = Integer.MAX_VALUE;
    Iterator<ExpressionEntry> exps = getExpressions();
    while (exps.hasNext()) {
      ExpressionEntry exp = exps.next();
      int expression = exp.getExpressionForSource(channel);
      if ((expression == ExpressionEntry.EXPRESSED) ||
          (expression == ExpressionEntry.WEAK_EXPRESSION)) {
        int time = exp.getTime();
        if (time < retval) {
          retval = time;
        }
      }
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Get the regional first expression time.  Returns null if not expressed
  ** FIXME: Upgrade to handle variable values!
  **
  */
  
  public Integer getRegionFirstExpressionTime(String regionID, ExpressionEntry.Source channel) {
    Integer retval = null;
    Iterator<ExpressionEntry> exps = getExpressions();
    while (exps.hasNext()) {
      ExpressionEntry exp = exps.next();
      if (DataUtil.keysEqual(regionID, exp.getRegion())) {
        int expression = exp.getExpressionForSource(channel); 
        if ((expression == ExpressionEntry.EXPRESSED) ||
            (expression == ExpressionEntry.WEAK_EXPRESSION)) {
          int time = exp.getTime();
          if ((retval == null) || (time < retval.intValue())) {
            retval = new Integer(time);
          }
        }
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the lineage first expression time.  Returns null if not expressed
  ** FIXME: Upgrade to handle variable values!
  **
  */
  
  public Integer getLineageFirstExpressionTime(List<TimeCourseData.TimeBoundedRegion> lineage, ExpressionEntry.Source channel) {
    Integer retval = null;
    Iterator<ExpressionEntry> exps = getExpressions();
    int numLin = lineage.size();
    while (exps.hasNext()) {
      ExpressionEntry exp = exps.next();
      int time = exp.getTime();
      String reg = exp.getRegion();
      for (int i = 0; i < numLin; i++) {
        TimeCourseData.TimeBoundedRegion tbr = lineage.get(i);         
        if (DataUtil.keysEqual(tbr.region, reg)) {
          if ((time >= tbr.getRegionStart()) && (time <= tbr.getRegionEnd())) {
            int expression = exp.getExpressionForSource(channel); 
            if ((expression == ExpressionEntry.EXPRESSED) ||
                (expression == ExpressionEntry.WEAK_EXPRESSION)) {
              if ((retval == null) || (time < retval.intValue())) {
                retval = new Integer(time);
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
  ** Get the set of regions where the gene expresses
  */
  
  public Set<String> expressesInRegions(ExpressionEntry.Source channel, boolean varToo) {
    HashSet<String> retval = new HashSet<String>();
    Iterator<ExpressionEntry> exps = getExpressions();
    while (exps.hasNext()) {
      ExpressionEntry exp = exps.next();
      int expression = exp.getExpressionForSource(channel); 
      if ((expression == ExpressionEntry.EXPRESSED) ||
          (expression == ExpressionEntry.WEAK_EXPRESSION)) {
        retval.add(exp.getRegion());
      } else if (varToo && (expression == ExpressionEntry.VARIABLE)) {
        double var = exp.getVariableLevelForSource(channel);
        if (var > 0.0) {
          retval.add(exp.getRegion());
        }
      }
    }
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Answer if the gene expresses in the given region
  **
  */
  
  public boolean expressesInRegion(String regionID, ExpressionEntry.Source channel, boolean varToo) {
    Iterator<ExpressionEntry> exps = getExpressions();
    while (exps.hasNext()) {
      ExpressionEntry exp = exps.next();
      if (DataUtil.keysEqual(regionID, exp.getRegion())) {
        int expression = exp.getExpressionForSource(channel); 
        if ((expression == ExpressionEntry.EXPRESSED) ||
            (expression == ExpressionEntry.WEAK_EXPRESSION)) {
          return (true);
        } else if (varToo && (expression == ExpressionEntry.VARIABLE)) {
          double var = exp.getVariableLevelForSource(channel);
          if (var > 0.0) {
            return (true);
          }
        }
      }
    }
    return (false);
  } 
  
  /***************************************************************************
  **
  ** Answer if the gene expresses in one and only one region.  Region ID
  ** if true, null otherwise.
  **
  */
  
  public String expressesInOnlyAndOnlyOneRegion(ExpressionEntry.Source channel, boolean varToo) {
    HashSet<String> regions = new HashSet<String>();
    Iterator<ExpressionEntry> exps = getExpressions();
    while (exps.hasNext()) {
      ExpressionEntry exp = exps.next();
      int expression = exp.getExpressionForSource(channel); 
      if ((expression == ExpressionEntry.EXPRESSED) ||
          (expression == ExpressionEntry.WEAK_EXPRESSION)) {
         String reg = exp.getRegion();     
         regions.add(reg);
      } else if (varToo && (expression == ExpressionEntry.VARIABLE)) {
        double var = exp.getVariableLevelForSource(channel);
        if (var > 0.0) {
          String reg = exp.getRegion();  
          regions.add(reg);
        }
      }
    }
    return ((regions.size() == 1) ? regions.iterator().next() : null);
  }  
  
  /***************************************************************************
  **
  ** Return the maximum time
  **
  */
  
  public int getMaximumTime() {
    TreeSet<Integer> times = new TreeSet<Integer>();
    getInterestingTimes(times);
    return (times.last().intValue());
  }  

  /***************************************************************************
  **
  ** Return the minimum time
  **
  */
  
  public int getMinimumTime() {
    TreeSet<Integer> times = new TreeSet<Integer>();
    getInterestingTimes(times);
    return (times.first().intValue());
  }  
  
  /***************************************************************************
  **
  ** Get the set of regions
  **
  */
  
  public void getRegions(Set<String> regions) {
    Iterator<ExpressionEntry> exps = getExpressions();
    while (exps.hasNext()) {
      ExpressionEntry exp = exps.next();
      regions.add(exp.getRegion());
    }
    return;
  }  

  /***************************************************************************
  **
  ** Get an HTML expression table suitable for display.
  */
  
  public int getExpressionTable(PrintWriter out, DataAccessContext dacx, TimeCourseData tcd, boolean showTree) {
    return ((new TimeCourseTableDrawer(dacx, this)).getExpressionTable(out, tcd, showTree));
  }

  /***************************************************************************
  **
  ** Write the expression tables as a csv file:
  **
  */
  
  public void exportCSV(PrintWriter out, Iterator<GeneTemplateEntry> tmpit, 
                        boolean encodeConfidence, boolean exportInternals) {
    if ((internalOnly_ && !exportInternals) || (data_.size() == 0)) {
      return;
    }   
    out.print("\"");
    out.print(name_);
    out.print("\"");
    if (tmpit.hasNext()) {
      out.print(",");
    }    
    while (tmpit.hasNext()) {
      GeneTemplateEntry gte = tmpit.next();
      ExpressionEntry exp = matchingEntry(gte.region, gte.time);
      int showConf = USE_BASE_CONFIDENCE;
      if (encodeConfidence) {
        int entryConfidence = exp.getConfidence();
        showConf = mapEntryConfidence(entryConfidence);
      }
      exp.exportCSV(out, showConf, encodeConfidence);
      if (tmpit.hasNext()) {
        out.print(",");
      }   
    }
    out.println();
    return;
  }
  
  /***************************************************************************
  **
  ** Get the exact matching entry
  */
  
  private ExpressionEntry matchingEntry(String region, int hour) {
    Iterator<ExpressionEntry> eit = getExpressions();
    while (eit.hasNext()) {
      ExpressionEntry ee = eit.next();
      int time = ee.getTime();      
      if (!DataUtil.keysEqual(ee.getRegion(), region))  {
        continue;
      }
      if (time == hour) {
        return (ee);
      }
    }
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Answer if we have ANY data!
  */
  
  public boolean haveData() {
    Iterator<ExpressionEntry> eit = getExpressions();
    while (eit.hasNext()) {
      ExpressionEntry ee = eit.next();
      int expression = ee.getExpressionForSource(ExpressionEntry.Source.NO_SOURCE_SPECIFIED);
      if ((expression != ExpressionEntry.NO_DATA) && (expression != ExpressionEntry.NO_REGION)) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Answer if we have ANY data!
  */
  
  public boolean haveDataForSource(ExpressionEntry.Source source) {
    Iterator<ExpressionEntry> eit = getExpressions();
    while (eit.hasNext()) {
      ExpressionEntry ee = eit.next();
      int expression = ee.getExpressionForSource(source);
      if ((expression != ExpressionEntry.NO_DATA) && (expression != ExpressionEntry.NO_REGION)) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Write the Time Course Gene to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<timeCourse gene=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    out.print("\" baseConfidence=\"");
    out.print(mapConfidence(confidence_));
    out.print("\" timeCourse=\"");
    out.print((hasTimeCourse_) ? "yes" : "no");
    if (timeCourseNote_ != null) {
      out.print("\" note=\"");
      out.print(CharacterEntityMapper.mapEntities(timeCourseNote_, false));
    }
    if (archival_) {
      out.print("\" archival=\"yes");
    }
    if (internalOnly_) {
      out.print("\" internalOnly=\"yes");
    }    
    out.println("\">");
    ind.up();    
    if (data_.size() > 0) {
      ind.indent();   
      out.println("<expData>");
      ind.up();    
      Iterator<ExpressionEntry> exps = getExpressions();
      while (exps.hasNext()) {
        ExpressionEntry exp = exps.next();
        exp.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</expData>");
    }
    if (pertGenes_.size() > 0) {
      ind.indent();   
      out.println("<perturbations>");
      ind.up();
      Iterator<PertSources> pkit = pertGenes_.keySet().iterator();
      while (pkit.hasNext()) {
        PertSources pss = pkit.next();
        PerturbedTimeCourseGene ptcg = pertGenes_.get(pss);
        ptcg.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</perturbations>");
    } 
    if (simData_.size() > 0) {
      ind.indent();   
      out.println("<simulations>");
      ind.up();
      Iterator<String> skit = new TreeSet<String>(simData_.keySet()).iterator();
      while (skit.hasNext()) {
        String sk = skit.next();
        ind.indent();   
        out.print("<simulation id=\"");
        out.print(sk);
        out.println("\">");
        ind.up();
        List<ExpressionEntry> sees = simData_.get(sk);
        Iterator<ExpressionEntry> exps = sees.iterator();
        while (exps.hasNext()) {
          ExpressionEntry exp = exps.next();
          exp.writeXML(out, ind);
        }
        ind.down().indent();
        out.println("</simulation>");
      }
      ind.down().indent();
      out.println("</simulations>");
    } 
    ind.down().indent();       
    out.println("</timeCourse>");
    return;
  }
    
  /***************************************************************************
  **
  ** Write the Time Course Gene to XML as a template
  **
  */
  
  public void writeXMLForTemplate(PrintWriter out, Indenter ind) {
    if (data_.size() == 0) {
      return;
    }
    ind.indent();    
    out.println("<timeCourse isTemplate=\"true\">");
    ind.up();    
    Iterator<ExpressionEntry> exps = getExpressions();
    while (exps.hasNext()) {
      ExpressionEntry exp = exps.next();
      exp.writeXML(out, ind);
    }
    ind.down().indent();       
    out.println("</timeCourse>");
    return;
  }
 
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  @Override
  public String toString() {
    return ("TimeCourseGene: name = " + name_ + " confidence = " + confidence_ +
            " hasTimeCourse = " + hasTimeCourse_ + " expressions = " + data_);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Map a confidence
  */
  
  public static int mapEntryConfidence(int baseConfidence, int entryConfidence) {
    if (entryConfidence == TimeCourseGene.USE_BASE_CONFIDENCE) {
      return (baseConfidence);
    } else {
      return (entryConfidence);
    }
  }

  /***************************************************************************
  **
  ** Reverse Map a confidence
  */
  
  public static int reverseMapEntryConfidence(int baseConfidence, int entryConfidence) {
    if (entryConfidence == baseConfidence) {
      return (TimeCourseGene.USE_BASE_CONFIDENCE);
    } else {
      return (entryConfidence);
    }
  }
  
  /***************************************************************************
  **
  ** Map the confidence value
  */
  
  public static String mapConfidence(int value) {
    switch (value) {
      case NORMAL_CONFIDENCE:
        return ("normal");
      case INTERPOLATED:
        return ("interpolated");
      case ASSUMPTION:
        return ("assumption");      
      case INFERRED:
        return ("inferred");
      case QUESTIONABLE:
        return ("questionable");
      default:
        throw new IllegalArgumentException();
    }    
  }  
  
  /***************************************************************************
  **
  ** Map to the confidence value
  */
  
  public static int mapToConfidence(String str) {
    if (str.equalsIgnoreCase("normal")) {
      return (NORMAL_CONFIDENCE);
    } else if (str.equalsIgnoreCase("interpolated")) {
      return (INTERPOLATED);   
    } else if (str.equalsIgnoreCase("assumption")) {
      return (ASSUMPTION);      
    } else if (str.equalsIgnoreCase("inferred")) {
      return (INFERRED);  
    } else if (str.equalsIgnoreCase("questionable")) {
      return (QUESTIONABLE);
    } else {
       throw new IllegalArgumentException();
    }
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static String simDataKeywordOfInterest() {
    return ("simulation");
  }
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static String keywordOfInterest() {
    return ("timeCourse");
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static TimeCourseGene buildFromXML(DataAccessContext dacx, String elemName, 
                                            Attributes attrs) throws IOException {
    if (!elemName.equals("timeCourse")) {
      return (null);
    }
    
    String gene = null; 
    String confidence = null; 
    String timeCourse = null; 
    String note = null;
    String archival = null;
    String internalOnly = null;
    String isTemplate = null;

    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("gene")) {
          gene = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("baseConfidence")) {
          confidence = val;
        } else if (key.equals("timeCourse")) {
          timeCourse = val;
        } else if (key.equals("note")) {
          note = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("archival")) {
          archival = val;
        } else if (key.equals("internalOnly")) {
          internalOnly = val;
        } else if (key.equals("isTemplate")) {
          isTemplate = val;
        }
      }
    }
    
    boolean makeTemplate = (new Boolean(isTemplate)).booleanValue();
    
    if (makeTemplate) {
      return (new TimeCourseGene(dacx, true));
    }
    if ((gene == null) || (confidence == null) || (timeCourse == null)) {
      throw new IOException();
    }
    return (new TimeCourseGene(dacx, gene, confidence, timeCourse, note, archival, internalOnly));
  }
  
  /***************************************************************************
  **
  ** Answer if there are strategy problems with an expression entry.
  */
  
  public static boolean strategyProblems(ExpressionEntry entry, ExpressionEntry.Source exprSource, Iterator<ExpressionEntry> eit, Map<String, Integer> prevStates) {  
   
    int startStrat = entry.getStartStrategy(exprSource);
    int endStrat = entry.getEndStrategy(exprSource);
    
    String region = entry.getRegion();
    Integer lastStateObj = prevStates.get(region);
    if (lastStateObj == null) {
      lastStateObj = new Integer(LAST_WAS_STANDARD);
      prevStates.put(region, lastStateObj);
    }
    int lastState = lastStateObj.intValue();
    
    if ((lastState == LAST_WAS_VARIABLE) &&  
        ((startStrat != ExpressionEntry.NO_STRATEGY_SPECIFIED) ||
         (endStrat != ExpressionEntry.NO_STRATEGY_SPECIFIED))) {
       return (true);
    }
    if ((lastState == LAST_HAD_STRATEGY) && (entry.getExpressionForSource(exprSource) == ExpressionEntry.VARIABLE)) {
      return (true);
    }
    
    int edgeType = edgeStatus(entry, exprSource, eit);
 
    if ((edgeType == NOT_AN_EDGE_) && 
        ((startStrat != ExpressionEntry.NO_STRATEGY_SPECIFIED) ||
         (endStrat != ExpressionEntry.NO_STRATEGY_SPECIFIED))) {
      return (true);
    } else if ((edgeType == START_AND_END_EDGE_) && 
               (ExpressionEntry.inconsistentStrategies(startStrat, endStrat) ||
                ExpressionEntry.isOffAtBoundary(startStrat) ||
                ExpressionEntry.isOffAtBoundary(endStrat))) {
      return (true);  
    } else if ((edgeType == START_EDGE_) &&
               (endStrat != ExpressionEntry.NO_STRATEGY_SPECIFIED)) {
      return (true);
      // Single-block regions cannot have an end strategy and can only have either no
      // start strategy or one that has the block start be off:
    } else if ((edgeType == SINGLE_BLOCK_REGION_) && 
               ((endStrat != ExpressionEntry.NO_STRATEGY_SPECIFIED) ||
                 (!(ExpressionEntry.isOffAtBoundary(startStrat) ||
                    (startStrat == ExpressionEntry.NO_STRATEGY_SPECIFIED))))) {
      return (true);
    } else if (((edgeType == END_EDGE_) || (edgeType == REGION_DISAPPEARING_)) &&
               (startStrat != ExpressionEntry.NO_STRATEGY_SPECIFIED)) {
      return (true);
    // } else if () {    FIX ME!!!!!!!!  
     // If the previous expression was a start edge, any "off" strategy it has
     // overrides any "off" strategy we would have as an end edge.  That is not
     // being caught!
    } else {      
      if ((startStrat != ExpressionEntry.NO_STRATEGY_SPECIFIED) || 
          (endStrat != ExpressionEntry.NO_STRATEGY_SPECIFIED)) {
        lastState = TimeCourseGene.LAST_HAD_STRATEGY;
      } else if (entry.getExpressionForSource(exprSource) == ExpressionEntry.VARIABLE) {
        lastState = TimeCourseGene.LAST_WAS_VARIABLE;
      }
      prevStates.put(region, new Integer(lastState));
      return (false);
    }
  }
            
  /***************************************************************************
  **
  ** Return the edge status of an expression entry.
  */
    
  public static int edgeStatus(ExpressionEntry entry, ExpressionEntry.Source exprSource, Iterator<ExpressionEntry> eit) {
    
    if (entry == null) {
      throw new IllegalArgumentException();
    }
    
    //
    // An expression entry represents a start edge if it specifies a non-zero
    // level of expression for a region, and there is either no earlier
    // expression entry for that region, or if the previous entry for that
    // region has no expression.  A similar approach is taken for end edges.
    //
    
    boolean entryExpressing = (entry.getExpressionForSource(exprSource) > ExpressionEntry.NOT_EXPRESSED);
    
    if (!entryExpressing) {
      return (NOT_AN_EDGE_);
    }
    
    String region = entry.getRegion();
    int hour = entry.getTime();
    
    ExpressionEntry greatestLower = null;
    ExpressionEntry leastHigher = null;    
    while (eit.hasNext()) {
      ExpressionEntry ee = eit.next();
      int time = ee.getTime();      
      if (!DataUtil.keysEqual(ee.getRegion(), region)) {
        continue;
      }
      if (time == hour) {
        continue; // Skip matching entry
      } else if (time > hour) {
        if ((leastHigher == null) || (time < leastHigher.getTime())) {
          leastHigher = ee;
        }
      } else if (time < hour) {
        if ((greatestLower == null) || (time > greatestLower.getTime())) {
          greatestLower = ee;
        }        
      }
    }

    //
    // Note these two cases will cause even variable expression with 0.0 value to be called
    // as "expressing".  But edge status is not relevant with variable entries stuff (no strategies)
    // 
    boolean earlierExpressing = (greatestLower != null) && 
                                (greatestLower.getExpressionForSource(exprSource) > ExpressionEntry.NOT_EXPRESSED);    
    
    boolean laterExpressing = (leastHigher != null) && 
                              (leastHigher.getExpressionForSource(exprSource) > ExpressionEntry.NOT_EXPRESSED);    

    if (earlierExpressing && laterExpressing) {
      return (NOT_AN_EDGE_);  // since we too are expressing  
    } else if (!earlierExpressing && laterExpressing) {    
      return (START_EDGE_);
    } else if ((greatestLower == null) && (leastHigher == null)) {
      return (SINGLE_BLOCK_REGION_);    
    } else if (earlierExpressing && (leastHigher == null)) {
      return (REGION_DISAPPEARING_);
    } else if (earlierExpressing && !laterExpressing) {
      return (END_EDGE_);
    } else if (!earlierExpressing && !laterExpressing) {
      return (START_AND_END_EDGE_);
    }
    
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** Get the set of source options
  */
  
  public static Set<ExpressionEntry.Source> getSourceOptions(Collection<ExpressionEntry> expr) {
    HashSet<ExpressionEntry.Source> retval = new HashSet<ExpressionEntry.Source>();
    Iterator<ExpressionEntry> eeit = expr.iterator();
    while (eeit.hasNext()) {
      ExpressionEntry ee = eeit.next();
      ExpressionEntry.Source qualSrc = ee.getQualifiedSource();
      if (qualSrc != null) {    
        retval.add(qualSrc);
      }
    }
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For collecting up bounds or matches
  */
 
  private static class ExpTriplet {
    ExpressionEntry greatestLower;
    ExpressionEntry matchingEntry;
    ExpressionEntry leastHigher;
    
    ExpTriplet() {
      greatestLower = null;
      matchingEntry = null;
      leastHigher = null;     
    }   
  }
  
  /***************************************************************************
  **
  ** Returns variable expression data
  **
  */
  
  public static class VariableLevel {
    public double level;
  }
}
