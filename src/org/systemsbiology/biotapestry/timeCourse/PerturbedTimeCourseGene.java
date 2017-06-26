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

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.List;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.perturb.PertSources;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Splitter;
import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** This holds spatial and temporal expression for a gene in a perturbed state
*/

public class PerturbedTimeCourseGene implements Cloneable, TimeCourseTableDrawer.Client {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private PertSources sources_;
  private ArrayList<ExpressionEntry> pertData_;
  private ArrayList<ExpressionEntry> controlData_;
  private String timeCourseNote_;  
  private int confidence_;
  private boolean internalOnly_;
  private PerturbationData pd_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public PerturbedTimeCourseGene(PerturbedTimeCourseGene other) {
    this(other, true);
  }  

  /***************************************************************************
  **
  ** Constructor
  */

  public PerturbedTimeCourseGene(PerturbedTimeCourseGene other, boolean doData) {
    this.pd_ = other.pd_;
    this.sources_ = other.sources_.clone();
    this.timeCourseNote_ = other.timeCourseNote_;  
    this.confidence_ = other.confidence_;
    this.internalOnly_ = other.internalOnly_;
    this.pertData_ = new ArrayList<ExpressionEntry>();
    if (doData) {
      int size = other.pertData_.size();
      for (int i = 0; i < size; i++) {
        ExpressionEntry exp = other.pertData_.get(i);
        this.pertData_.add(exp.clone());
      }
    }
    boolean haveControl = (other.controlData_ != null);
    this.controlData_ = (haveControl) ? new ArrayList<ExpressionEntry>() : null;
    if (doData && haveControl) {
      int size = other.controlData_.size();
      for (int i = 0; i < size; i++) {
        ExpressionEntry exp = other.controlData_.get(i);
        this.controlData_.add(exp.clone());
      }
    }
  }   

  /***************************************************************************
  **
  ** Constructor
  */

  public PerturbedTimeCourseGene(PerturbationData pd, PertSources sources, int confidence, boolean internalOnly) {
    pd_ = pd;
    sources_ = sources.clone();
    confidence_ = confidence;
    internalOnly_ = internalOnly;
    pertData_ = new ArrayList<ExpressionEntry>();
    controlData_ = null;
  }

  /***************************************************************************
  **
  ** Constructor
  */

  public PerturbedTimeCourseGene(PerturbationData pd, PertSources sources, Iterator<GeneTemplateEntry> tempit) {
    pd_ = pd;
    sources_ = sources.clone();
    confidence_ = TimeCourseGene.NORMAL_CONFIDENCE;
    internalOnly_ = false;
    pertData_ = new ArrayList<ExpressionEntry>();
    while (tempit.hasNext()) {
      GeneTemplateEntry gte = tempit.next();
      ExpressionEntry exp = new ExpressionEntry(gte.region, gte.time, 
                                                ExpressionEntry.NO_DATA, 
                                                TimeCourseGene.USE_BASE_CONFIDENCE);
      pertData_.add(exp);
    }
    controlData_ = null;
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public PerturbedTimeCourseGene(PerturbationData pd, String srcs, String confidence, String note, String internalOnly) 
    throws IOException {
   
    if (srcs == null) {
      throw new IOException();
    }
    pd_ = pd;
    sources_ = new PertSources(pd);
    List<String> srcList = Splitter.stringBreak(srcs, ",", 0, false);
    int numSrc = srcList.size();
    for (int i = 0; i < numSrc; i++){
      String srcID = srcList.get(i);
      sources_.addSourceID(srcID);
    }
  
    if (confidence == null) {
      confidence_ = TimeCourseGene.NORMAL_CONFIDENCE;
    } else {
      try {
        confidence_ = TimeCourseGene.mapToConfidence(confidence.trim());
      } catch (IllegalArgumentException iaex) {
        throw new IOException();
      }
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
    pertData_ = new ArrayList<ExpressionEntry>();
    controlData_ = null;
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
    if (!(other instanceof PerturbedTimeCourseGene)) {
      return (false);
    }
    PerturbedTimeCourseGene otherPTCG = (PerturbedTimeCourseGene)other;
  
    if (!DataUtil.stringsEqual(this.timeCourseNote_, otherPTCG.timeCourseNote_)) {
      return (false);
    }
    if (this.confidence_ != otherPTCG.confidence_) {
      return (false);
    }
    if (this.internalOnly_ = !otherPTCG.internalOnly_) {
      return (false);
    }

    if (!this.pertData_.equals(otherPTCG.pertData_)) {
      return (false);
    }
    
    if (!this.controlData_.equals(otherPTCG.controlData_)) {
      return (false);
    }
    
    if (!this.sources_.equals(otherPTCG.sources_)) {
      return (false);
    }
    
    return (true);
  }
 
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public PerturbedTimeCourseGene clone() {
    try {
      PerturbedTimeCourseGene retval = (PerturbedTimeCourseGene)super.clone();
      int size = this.pertData_.size();
      retval.pertData_ = new ArrayList<ExpressionEntry>();
      for (int i = 0; i < size; i++) {
        ExpressionEntry exp = this.pertData_.get(i);
        retval.pertData_.add(exp.clone());
      }
      if (this.controlData_ != null) {
        retval.controlData_ = new ArrayList<ExpressionEntry>();
        for (int i = 0; i < size; i++) {
          ExpressionEntry exp = this.controlData_.get(i);
          retval.controlData_.add(exp.clone());
        }
      }
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
   
  /***************************************************************************
  **
  ** Get the sources
  */
  
  public String getName() {
    return (sources_.getDisplayString(pd_, PertSources.NO_FOOTS));
  }  
  
  /***************************************************************************
  **
  ** Get the sources
  */
  
  public PertSources getPertSources() {
    return (sources_);
  }
  
  /***************************************************************************
  **
  ** Set the Sources
  */
  
  public void setSources(PertSources sources) {
    sources_ = sources;
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
    return (TimeCourseGene.mapEntryConfidence(confidence_, entryConfidence));
  }

  /***************************************************************************
  **
  ** Reverse Map a confidence
  */
  
  public int reverseMapEntryConfidence(int entryConfidence) {
    return (TimeCourseGene.reverseMapEntryConfidence(confidence_, entryConfidence));
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
  ** set to handle separate control expression
  */
  
  public void setForDistinctControlExpr() {
    controlData_ = new ArrayList<ExpressionEntry>();
    return;
  }
  
  /***************************************************************************
  **
  ** set to handle separate control expression AND STOCK IT
  */

  public void setForDistinctControlExpr(Iterator<GeneTemplateEntry> tempit) {
    controlData_ = new ArrayList<ExpressionEntry>();
    while (tempit.hasNext()) {
      GeneTemplateEntry gte = tempit.next();
      ExpressionEntry exp = new ExpressionEntry(gte.region, gte.time, 
                                                ExpressionEntry.NO_DATA, 
                                                TimeCourseGene.USE_BASE_CONFIDENCE);
      controlData_.add(exp);
    }
    return;
  }  
 
  /***************************************************************************
  **
  ** Drop separate control expression
  */
  
  public void dropDistinctControlExpr() {
    controlData_ = null;
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if we are carrying distinct control expression
  */
  
  public boolean usingDistinctControlExpr() {
    return (controlData_ != null);
  }
  
  /***************************************************************************
  **
  ** Add an expression entry
  */
  
  public void addExpression(ExpressionEntry expr, ExpressionEntry ctrlExpr) {
    pertData_.add(expr);
    if (ctrlExpr != null) {
      if (controlData_ == null) {
        throw new IllegalArgumentException();
      }
      controlData_.add(ctrlExpr);
    }
    return;
  }
  
 /***************************************************************************
  **
  ** Add a control expression entry
  */
  
  public void addCtrlExpression(ExpressionEntry ctrlExpr) {
    if (controlData_ == null) {
      throw new IllegalArgumentException();
    }
    controlData_.add(ctrlExpr);
    return;
  }
 
  /***************************************************************************
  **
  ** Replace all expressions
  */
  
  public void replaceExpressions(List<ExpressionEntry> exprs, List<ExpressionEntry> ctrlExpr) {
    pertData_.clear();
    pertData_.addAll(exprs);
    if (ctrlExpr != null) {
      if (controlData_ == null) {
        throw new IllegalArgumentException();
      }
      controlData_.clear();
      controlData_.addAll(ctrlExpr);
    }  
    return;
  }
  
  /***************************************************************************
  **
  ** Add an expression entry
  */
  
  public void addExpression(int index, ExpressionEntry expr, ExpressionEntry ctrlExpr) {
    pertData_.add(index, expr);
    if (ctrlExpr != null) {
      if (controlData_ == null) {
        throw new IllegalArgumentException();
      }
      controlData_.add(index, ctrlExpr);
    }  
    return;
  }
  
  /***************************************************************************
  **
  ** Delete an expression entry at the given index
  */
  
  public void deleteExpression(int i) {
    pertData_.remove(i);
    if (controlData_ != null) {
      controlData_.remove(i);
    }
    return;
  }  

  /***************************************************************************
  **
  ** Get the nth expression
  */
  
  public ExpressionEntry getExpression(int n) {
    return (pertData_.get(n));
  }    
  
  /***************************************************************************
  **
  ** Get the nth control expression
  */
  
  public ExpressionEntry getControlExpression(int n) {
    if (controlData_ == null) {
      throw new IllegalStateException();
    }
    return (controlData_.get(n));
  }    
  
  /***************************************************************************
  **
  ** Get an iterator over the expressions
  */
  
  public Iterator<ExpressionEntry> getExpressions() {
    return (pertData_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the control expressions
  */
  
  public Iterator<ExpressionEntry> getControlExpressions() {
    if (controlData_ == null) {
      throw new IllegalStateException();
    }
    return (controlData_.iterator());
  }
   
  /***************************************************************************
  **
  ** Modify existing template entry
  */
  
  public void updateExpression(int i, GeneTemplateEntry newEntry) {
    ExpressionEntry ee = getExpression(i);
    ee.updateRegionAndTime(newEntry.region, newEntry.time);
    if (controlData_ != null) {
      ee = getControlExpression(i);
      ee.updateRegionAndTime(newEntry.region, newEntry.time);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Return the expression level for a given region and hour.
  */
  
  public int getExpressionLevelForSource(String region, int hour, ExpressionEntry.Source exprSource, TimeCourseGene.VariableLevel varLev, double weak) {
    Iterator<ExpressionEntry> eit = getExpressions();
    return (getExpressionLevelGuts(eit, region, hour, exprSource, varLev));
  } 
  
  /***************************************************************************
  **
  ** Return the control expression level for a given region and hour.
  */
  
  public int getControlExpressionLevelForSource(String region, int hour, ExpressionEntry.Source exprSource, TimeCourseGene.VariableLevel varLev, double weak) {
    Iterator<ExpressionEntry> eit = getControlExpressions();
    return (getExpressionLevelGuts(eit, region, hour, exprSource, varLev));
  } 
  
  /***************************************************************************
  **
  ** Return the expression level for a given region and hour.
  */
  
  private int getExpressionLevelGuts(Iterator<ExpressionEntry> eit, String region, int hour, 
                                     ExpressionEntry.Source exprSource, TimeCourseGene.VariableLevel varLev) {
    while (eit.hasNext()) {
      ExpressionEntry ee = eit.next();
      int time = ee.getTime(); 
      if (DataUtil.keysEqual(ee.getRegion(), region) && (hour == time))  {
        int matchExpr = ee.getExpressionForSource(exprSource);
        if (matchExpr == ExpressionEntry.VARIABLE) {
          varLev.level = ee.getVariableLevelForSource(exprSource);
        }
        return (matchExpr);
      }
    }
    return (ExpressionEntry.NO_REGION);
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
        return ((confidence == TimeCourseGene.USE_BASE_CONFIDENCE) ? confidence_ : confidence);
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
  ** Write the PerturbedTime Course Gene to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<perturbExpr srcs=\"");
    out.print(sources_.sourcesAsString());
    out.print("\" baseConfidence=\"");
    out.print(TimeCourseGene.mapConfidence(confidence_));
    if (timeCourseNote_ != null) {
      out.print("\" note=\"");
      out.print(CharacterEntityMapper.mapEntities(timeCourseNote_, false));
    }
    if (internalOnly_) {
      out.print("\" internalOnly=\"yes");
    }    
    out.println("\">");
    ind.up();    
    if (pertData_.size() > 0) {
      ind.indent();    
      out.println("<perts>");
      ind.up();
      Iterator<ExpressionEntry> exps = getExpressions();
      while (exps.hasNext()) {
        ExpressionEntry exp = exps.next();
        exp.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</perts>");
    }
    ind.down();
    
    ind.up();    
    if ((controlData_ != null) && (controlData_.size() > 0)) {
      ind.indent();    
      out.println("<ctrls>");
      ind.up();
      Iterator<ExpressionEntry> exps = getControlExpressions();
      while (exps.hasNext()) {
        ExpressionEntry exp = exps.next();
        exp.writeXML(out, ind);
      }
      ind.down().indent();
      out.println("</ctrls>");
    }
    ind.down().indent();       
    out.println("</perturbExpr>");
    return;
  }
  
  /***************************************************************************
  **
  ** Get an HTML expression table suitable for display.
  */
  
  public int getExpressionTable(PrintWriter out, TimeCourseGene wtGene, TimeCourseData tcd, DataAccessContext dacx) {
    return ((new TimeCourseTableDrawer(dacx, dacx.getRMan(), wtGene, this)).getPertExpressionTable(out, tcd));
  }
     
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("PerturbedTimeCourseGene: sources = " + sources_ + " confidence = " + 
            confidence_ + " expressions = " + pertData_ + " expressions = " + controlData_);
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
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("perturbExpr");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the keyword for distinct control expressions
  */
  
  public static String keyForControlCollection() {
    return ("ctrls");
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static PerturbedTimeCourseGene buildFromXML(DataAccessContext dacx, String elemName, 
                                                     Attributes attrs) throws IOException {
    if (!elemName.equals("perturbExpr")) {
      return (null);
    }
    
    String pertSrcs = null;
    String confidence = null; 
    String note = null;
    String internalOnly = null;

    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("baseConfidence")) {
          confidence = val;
        } else if (key.equals("srcs")) {
          pertSrcs = val;
        } else if (key.equals("note")) {
          note = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("internalOnly")) {
          internalOnly = val;
        }
      }
    }

    if ((confidence == null) || (pertSrcs == null)) {
      throw new IOException();
    }
    
    PerturbationData pd = dacx.getExpDataSrc().getPertData();
    return (new PerturbedTimeCourseGene(pd, pertSrcs, confidence, note, internalOnly));
  }
}
