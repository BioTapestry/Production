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

package org.systemsbiology.biotapestry.qpcr;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.MinMax;

/****************************************************************************
**
** This holds QPCR Time spans
*/

class TimeSpan {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
   static final String XML_TAG_REGION_RESTRICT = "regionRestriction";
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private int minTime_;
  private int maxTime_;  
  private ArrayList<Batch> batches_;
  private ArrayList<String> regionRestrictions_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Constructor
  */

   TimeSpan(int minVal, int maxVal) {
    minTime_ = minVal;
    maxTime_ = maxVal;
    batches_ = new ArrayList<Batch>();
    regionRestrictions_ = new ArrayList<String>(); 
  }  
  
 /***************************************************************************
  **
  ** Constructor
  */

   TimeSpan(MinMax mm) {
    minTime_ = mm.min;
    maxTime_ = mm.max;
    batches_ = new ArrayList<Batch>();
    regionRestrictions_ = new ArrayList<String>();    
  }    

  /***************************************************************************
  **
  ** Copy Constructor
  */

   TimeSpan(TimeSpan other) {
    this.minTime_ = other.minTime_;
    this.maxTime_ = other.maxTime_;    
    this.batches_ = new ArrayList<Batch>();
    Iterator<Batch> obit = other.batches_.iterator();
    while (obit.hasNext()) {
      this.batches_.add(new Batch(obit.next()));
    }
    this.regionRestrictions_ = new ArrayList<String>();
    Iterator<String> orrit = other.regionRestrictions_.iterator();
    while (orrit.hasNext()) {
      this.regionRestrictions_.add(orrit.next());
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the non-standard times for the span 
  */
  
   void getNonStandardTimes(Set<Integer> nonStd) {
    Iterator<Batch> bit = getBatches();
    while (bit.hasNext()) {
      Batch b = bit.next();
      b.getNonStandardTimes(nonStd, minTime_, maxTime_);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if span has a standard time measurement
  */
  
   boolean hasStandardTime() {
    Iterator<Batch> bit = getBatches();
    while (bit.hasNext()) {
      Batch b = bit.next();
      if (b.hasStandardTime(minTime_, maxTime_)) {
        return (true);
      }
    }
    return (false);
  }
  
  /***************************************************************************
  **
  ** Get the span
  **
  */
  
   String getSpanAsString(TimeAxisDefinition tad) { 
    // E.g. "23-28 h"
    return (spanToString(tad, new MinMax(minTime_, maxTime_)));
  }
  
  /***************************************************************************
  **
  ** Get the span
  **
  */
  
   MinMax getMinMaxSpan() {
    return (new MinMax(minTime_, maxTime_));
  }  

  /***************************************************************************
  **
  ** Add a batch of results
  **
  */
  
   public void addBatch(Batch batch) {
    batches_.add(batch);
    return;
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the batches
  */
  
   Iterator<Batch> getBatches() {
    return (batches_.iterator());
  }   
  
  /***************************************************************************
  **
  ** Get an iterator over the batches for a particular time
  */
  
   Iterator<Batch> getBatchesForTime(int time) {
    ArrayList<Batch> retvalBase = new ArrayList<Batch>();
    Iterator<Batch> bit = batches_.iterator();
    while (bit.hasNext()) {
      Batch b = bit.next();
      int myTime = b.getTimeNumber();
      if (myTime == time) {
        retvalBase.add(b);
      }
    }
    return (retvalBase.iterator());
  }     
 
  /***************************************************************************
  **
  ** Get the batch count
  */
  
   int getBatchCount() {
    return (batches_.size());
  }
  
  /***************************************************************************
  **
  ** Delete a batch
  */
  
   void deleteBatch(int index) {
    batches_.remove(index);
    return;
  }
  
  /***************************************************************************
  **
  ** Get a batch
  */
  
   Batch getBatch(int index) {
    return (batches_.get(index));
  }
  
  /***************************************************************************
  **
  ** Get a batch with the given batch key, null if none exists.
  */
  
   public Batch getBatchWithIDAndInvest(String batchID, String invest) {
    int nsb = batches_.size();
    for (int i = 0; i < nsb; i++) {
      Batch checkit = batches_.get(i);
      String batchKey = checkit.getBatchKey();
      if (batchKey == null) {
        continue;
      }
      if (batchKey.equals(batchID)) {
        String chkInvest = checkit.getInvestigators();
        if (chkInvest == null) {
          if (invest == null) {
            return (checkit);
          }
        } else if (chkInvest.equals(invest)) {
          return (checkit);
        }
      }
    }
    return (null);
  }    

  /***************************************************************************
  **
  ** Add a region restriction
  **
  */
  
   void addRegionRestriction(String regionID) {
    regionRestrictions_.add(regionID);
    return;
  }
  
  /***************************************************************************
  **
  ** Delete a region restriction
  */
  
   void deleteRegionRestriction(int index) {
    regionRestrictions_.remove(index);
    return;
  }
  
  /***************************************************************************
  **
  ** Clear all region restrictions
  */
  
   void clearRegionRestrictions() {
    regionRestrictions_.clear();
    return;
  }  
  
  /***************************************************************************
  **
  ** Get an iterator over the region restrictions
  */
  
   Iterator<String> getRegionRestrictions() {
    return (regionRestrictions_.iterator());
  } 
  
  /***************************************************************************
  **
  ** Get the specified region restriction
  */
  
   String getRegionRestriction(int index) {
    return (regionRestrictions_.get(index));
  }   
   
  /***************************************************************************
  **
  ** Answer if there are any region restrictions
  */
  
   boolean haveRegionRestrictions() {
    return (regionRestrictions_.size() > 0);
  }

  /***************************************************************************
  **
  ** Replace a region restriction
  */
  
   void replaceRegionRestriction(int index, String reg) {
    regionRestrictions_.set(index, reg);
    return;
  }  
   
  /***************************************************************************
  **
  ** Get the set of footnote numbers used by this span
  */
  
   Set<String> getFootnoteNumbers() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Batch> bats = getBatches();
    while (bats.hasNext()) {
      Batch bat = bats.next();
      Set<String> batNotes = bat.getFootnoteNumbers();
      retval.addAll(batNotes);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get the time profile.  As we have evolved, there may be multiple batch
  ** times embedded in the time span. In legacy cases, there my be batches
  ** with no time stamp; it is assumed they are in the time period of this
  ** span.  But also to support legacy, there may be times of specific measurement
  ** outside of the time span.  All this to support weirdness in data seen over
  ** the years.
  **
  */
  
 SpanTimeProfile getBatchTimeProfile() {
 
   SpanTimeProfile retval = new SpanTimeProfile();
   Iterator<Batch> bit = getBatches();
    while (bit.hasNext()) {
      Batch b = bit.next();
      int time = b.getTimeNumber();
      if (time == Batch.NO_TIME) {
        retval.hasUntimedBatches = true;
      } else {
        retval.batchTimes.add(new Integer(time));
      }
    }
    getNonStandardTimes(retval.nonStandardTimes);
    return (retval);
  }

  /***************************************************************************
  **
  ** Write the time span to HTML
  **
  */
  
   void writeHTML(PrintWriter out, Indenter ind, QpcrTablePublisher qtp, SpanTimeProfile spt, TimeAxisDefinition tad) {
    ind.indent();
    qtp.paragraph(false);
    //
    // Some time spans get multiple time points in them.  If we have multiple
    // time points, issue in time order;
    // 
    int timedBatchCount = spt.batchTimes.size();
    boolean multiTimes = (timedBatchCount > 1);
    if (multiTimes) {
      Iterator<Integer> btit = spt.batchTimes.iterator();
      while (btit.hasNext()) {     
        Integer batchTime = btit.next();
        Iterator<Batch> bftit = getBatchesForTime(batchTime.intValue());
        if (bftit.hasNext()) {
          out.print("(");
          writeDetailedHTML(bftit, out, ind, qtp, batchTime, tad);
          out.print(" [");
          String tdisp = TimeAxisDefinition.getTimeDisplay(tad, batchTime, true, true);
          tdisp = tdisp.replaceAll(" ", "&nbsp;");
          out.print(tdisp);
          out.print("])");
          out.print("</p>");
          qtp.paragraph(false);
        }   
      }
      Iterator<Batch> bit = getBatchesForTime(Batch.NO_TIME);
      writeDetailedHTML(bit, out, ind, qtp, null, tad);
    } else {
      Iterator<Batch> bit = getBatches();
      writeDetailedHTML(bit, out, ind, qtp, null, tad);
    }   
    out.println("</p>");
    return;
  }
  
  
  /***************************************************************************
  **
  ** Handle loop details for batch set
  **
  */
  
  private void writeDetailedHTML(Iterator<Batch> bit, PrintWriter out, Indenter ind,
                                 QpcrTablePublisher qtp, Integer batchTime, TimeAxisDefinition tad) {
    while (bit.hasNext()) {
      Batch b = bit.next();
      b.writeHTML(out, ind, qtp, batchTime, tad);
      if (bit.hasNext()) {
        out.print("/");
        qtp.breakSpace();
      }
    }
    return;
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
    HashSet<String> retval = new HashSet<String>();
    retval.add("timeSpan");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the region restriction keyword
  **
  */
  
   static String regionRestrictKeyword() {
    return (XML_TAG_REGION_RESTRICT);
  }  
 
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
   public String toString() {
    return ("TimeSpan: min = " + minTime_ + " max = " + maxTime_ + " batches = " + batches_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static TimeSpan buildFromXML(TimeAxisDefinition tad, String elemName, 
                                Attributes attrs) throws IOException {
    if (!elemName.equals("timeSpan")) {
      return (null);
    }
    
    String legacySpan = null;
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
        if (key.equals("span")) {
          legacySpan = val;
        } else if (key.equals("min")) {
          minValStr = val;
        } else if (key.equals("max")) {
          maxValStr = val;
        }        
      }
    }
    
    if (legacySpan != null) {
      return (new TimeSpan(tad, legacySpan));
    } else if (minValStr != null) {
      try {
        int minVal = Integer.parseInt(minValStr);
        int maxVal = minVal;
        if (maxValStr != null) {
          maxVal = Integer.parseInt(maxValStr);
        }
        if (!spanIsOk(tad, minVal, maxVal)) {
          throw new IOException();
        } 
        return (new TimeSpan(minVal, maxVal));
      } catch (NumberFormatException ex) {
        throw new IOException();
      }
    } else {
      throw new IOException();
    }
  }
  
  /***************************************************************************
  **
  ** Process region restrictions from XML
  **
  */
  
   static String getRegionRestrictFromXML(String elemName, 
                                                Attributes attrs) throws IOException {
    
     String region = AttributeExtractor.extractAttribute(elemName, attrs, XML_TAG_REGION_RESTRICT, "region", true);
     return (region);    
  }
  
  /***************************************************************************
  **
  ** Get the minmax as a string
  **
  */
  
   static String spanToString(TimeAxisDefinition tad, MinMax bounds) {
    // E.g. "23-28 h"
    String displayUnitAbbrev = tad.unitDisplayAbbrev();
    
    String minStr;
    String maxStr;
    boolean abbreviate = false;  // Make this an argument???
    if (tad.haveNamedStages()) {
      TimeAxisDefinition.NamedStage minStage = tad.getNamedStageForIndex(bounds.min);
      TimeAxisDefinition.NamedStage maxStage = tad.getNamedStageForIndex(bounds.max);
      minStr = (abbreviate) ? minStage.abbrev : minStage.name;
      maxStr = (abbreviate) ? maxStage.abbrev : maxStage.name;      
    } else {
      minStr = Integer.toString(bounds.min);
      maxStr = Integer.toString(bounds.max);
    }    
    
    StringBuffer buf = new StringBuffer();
    if (tad.unitsAreASuffix()) {
      buf.append(minStr);
      if (!minStr.equals(maxStr)) {
        buf.append("-");
        buf.append(maxStr);
      }
      buf.append(" ");
      buf.append(displayUnitAbbrev);
    } else {
      buf.append(displayUnitAbbrev);
      buf.append(" ");
      buf.append(minStr);
      if (!minStr.equals(maxStr)) {
        buf.append("-");
        buf.append(maxStr);
      }
    }
    return (buf.toString());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
   
  /***************************************************************************
  **
  ** Used to specify batch time characteristics
  */
  
  static class SpanTimeProfile {
    TreeSet<Integer> batchTimes;
    HashSet<Integer> nonStandardTimes;
    boolean hasUntimedBatches;
    
    SpanTimeProfile() {
      batchTimes = new TreeSet<Integer>();
      nonStandardTimes = new HashSet<Integer>();
      hasUntimedBatches = false;
    }
  }   
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor (Legacy IO only)
  */

  private TimeSpan(TimeAxisDefinition tad, String legacySpan) throws IOException {
    try {
      minTime_ = QPCRData.getMinimum(legacySpan); 
      maxTime_ = QPCRData.getMaximum(legacySpan);
    } catch (IllegalArgumentException ex) {
      throw new IOException();
    }
    if (!spanIsOk(tad, minTime_, maxTime_)) {
      throw new IOException();
    }    
    batches_ = new ArrayList<Batch>();
    regionRestrictions_ = new ArrayList<String>();    
  } 

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CLASS  METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Checks span validity
  */

  private static boolean spanIsOk(TimeAxisDefinition tad, int min, int max) {
    if (!tad.spanIsOk(min, max)) {      
      return (false);
    }
    return (true);
  }
}
