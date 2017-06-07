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

package org.systemsbiology.biotapestry.qpcr;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.io.IOException;

import java.util.List;
import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.Splitter;

/****************************************************************************
**
** This holds QPCR Null targets
*/

class NullTarget implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String target_;
  private String region_;
  private ArrayList<NullTimeSpan> spans_;
  private ArrayList<Batch> supportBatches_;
  private String footsForHTML_;  // post QPCR-deprecation addition
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

   NullTarget() {
    spans_ = new ArrayList<NullTimeSpan>();
    supportBatches_ = new ArrayList<Batch>();
  }  

  /***************************************************************************
  **
  ** Constructor
  */

   NullTarget(String target) {
    target_ = target;
    spans_ = new ArrayList<NullTimeSpan>();
    supportBatches_ = new ArrayList<Batch>();    
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

   NullTarget(String target, String region) {
    target_ = target;
    region_ = region;
    spans_ = new ArrayList<NullTimeSpan>();
    supportBatches_ = new ArrayList<Batch>();    
  }  
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

   NullTarget(NullTarget other) {
    mergeInNewValues(other);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public NullTarget clone() {
    try {
      NullTarget newVal = (NullTarget)super.clone();
      newVal.spans_ = new ArrayList<NullTimeSpan>();
      Iterator<NullTimeSpan> osit = this.spans_.iterator();
      while (osit.hasNext()) {
        newVal.spans_.add(osit.next().clone());
      }
      newVal.supportBatches_ = new ArrayList<Batch>();
      Iterator<Batch> sbit = this.supportBatches_.iterator();
      while (sbit.hasNext()) {
        newVal.supportBatches_.add(sbit.next().clone());
      }
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }

  /***************************************************************************
  **
  ** Merge in new values
  */
  
  void mergeInNewValues(NullTarget other) {
    this.target_ = other.target_;
    this.region_ = other.region_;
    this.spans_ = new ArrayList<NullTimeSpan>();
    Iterator<NullTimeSpan> osit = other.spans_.iterator();
    while (osit.hasNext()) {
      this.spans_.add(osit.next().clone());
    }
    this.supportBatches_ = new ArrayList<Batch>();
    Iterator<Batch> sbit = other.supportBatches_.iterator();
    while (sbit.hasNext()) {
      this.supportBatches_.add(sbit.next().clone());
    }
    this.footsForHTML_ = other.footsForHTML_;
    return;
  } 
  
  /***************************************************************************
  **
  ** Update the data range
  */
  
   void updateDataRange(NullTimeSpan defaultSpan, int[] range) {
    int myMin = Integer.MAX_VALUE;
    int myMax = Integer.MIN_VALUE;    
    
    if (spans_.isEmpty()) {
      myMin = defaultSpan.getMin();
      myMax = defaultSpan.isASpan() ? defaultSpan.getMax() : myMin;
    } else {
      int numSpan = spans_.size();
      for (int i = 0; i < numSpan; i++) {
        NullTimeSpan nts = spans_.get(i);
        int ntsMin = nts.getMin();
        int ntsMax = nts.isASpan() ? nts.getMax() : ntsMin;      
        if (ntsMin < myMin) {
          myMin = ntsMin;
        }
        if (ntsMax > myMax) {
          myMax = ntsMax;
        }
      }
    }    
    if (myMin < range[0]) {
      range[0] = myMin;
    }
    if (myMax > range[1]) {
      range[1] = myMax;
    }    
    return;
  }
  
  /***************************************************************************
  **
  ** Get the target
  **
  */
  
   String getTarget() {
    return (target_);
  }
  
  /***************************************************************************
  **
  ** Get the region restriction
  **
  */
  
   String getRegionRestriction() {
    return (region_);
  }  
  
  /***************************************************************************
  **
  ** Set the target
  */
  
   void setTarget(String target) {
    target_ = target;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the region restriction
  */
  
   void setRegionRestriction(String region) {
    region_ = region;
    return;
  }  
   
  /***************************************************************************
  **
  ** Set the region restriction as footnotes
  */
  
   void setFootsForHTML(String foots) {
    footsForHTML_ = foots;
    return;
  } 
   
  /***************************************************************************
  **
  ** Get the list of footnote numbers used 
  */
  
   List<String> getFootnoteNumbers() {
    if (footsForHTML_ != null) {
      return (Splitter.stringBreak(footsForHTML_, ",", 0, true));   
    } else {
      return (new ArrayList<String>());
    }
  }
   
  /***************************************************************************
  **
  ** Add a time span
  **
  */
  
   void addTimeSpan(NullTimeSpan span) {
    spans_.add(span);
    return;
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the time spans
  */
  
   Iterator<NullTimeSpan> getTimes() {
    return (spans_.iterator());
  }

  /***************************************************************************
  **
  ** Get spans in a set
  */
  
   Set<NullTimeSpan> getSpansInSet() {
    return (new HashSet<NullTimeSpan>(spans_));
  }
  
  /***************************************************************************
  **
  ** Get the time span count
  */
  
   int getTimesCount() {
    return (spans_.size());
  }
  
  /***************************************************************************
  **
  ** Delete a time span
  */
  
   void deleteTimeSpan(int index) {
    spans_.remove(index);
    return;
  }

  /***************************************************************************
  **
  ** Drop all time spans
  */
  
   void dropTimeSpans() {
    spans_.clear();
    return;
  }  
  
  /***************************************************************************
  **
  ** Get a time span
  */
  
   NullTimeSpan getTimeSpan(int index) {
    return (spans_.get(index));
  }
  
  /***************************************************************************
  **
  ** Replace a time span
  */
  
   void replaceTimeSpan(int index, NullTimeSpan span) {
    spans_.set(index, span);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a batch of results
  **
  */
  
   public void addBatch(Batch batch) {
    supportBatches_.add(batch);
    return;
  }
  
  /***************************************************************************
  **
  ** Delete a batch
  */
  
   void deleteBatch(int index) {
    supportBatches_.remove(index);
    return;
  }
  /***************************************************************************
  **
  ** Get a batch
  */
  
   Batch getBatch(int index) {
    return (supportBatches_.get(index));
  }
  
  /***************************************************************************
  **
  ** Get the batch count
  */
  
   int getBatchCount() {
    return (supportBatches_.size());
  }   
  
  /***************************************************************************
  **
  ** Get an iterator over the support data
  */
  
   Iterator<Batch> getSupportData() {
    return (supportBatches_.iterator());
  }  
 
  /***************************************************************************
  **
  ** Write the null target as a single string
  **
  */
  
   String displayString(boolean fullyQualified, String units, boolean isSuffix) {
    StringBuffer buf = new StringBuffer();
    buf.append(target_);

    if (footsForHTML_ != null) {
      buf.append("<sup>");
      buf.append(footsForHTML_);
      buf.append("</sup>");
    }
    if (!fullyQualified) {
      return (buf.toString());
    }
    buf.append(" (");
    buf.append(displayTimeSpansString(units, isSuffix));
    buf.append(")");
    return (buf.toString());
  }
  
  /***************************************************************************
  **
  ** Write the time spans
  **
  */
  
   String displayTimeSpansString(String units, boolean isSuffix) {
    StringBuffer buf = new StringBuffer();
    boolean isFirst = true;
    if (!isSuffix) {
      buf.append(units);
      buf.append(" ");
    }
    ArrayList<NullTimeSpan> sorted = new ArrayList<NullTimeSpan>(spans_);
    Collections.sort(sorted);
    Iterator<NullTimeSpan> tsit = sorted.iterator();
    while (tsit.hasNext()) {
      NullTimeSpan nts = tsit.next();          
      if (isFirst) {
        isFirst = false;
      } else {
        buf.append(", ");
      }
      buf.append(nts.displayString());
    }
    if (isSuffix) {
      buf.append(" ");
      buf.append(units);
    }
    return (buf.toString());
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
    retval.add("nullTarget");
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
   public String toString() {
    return ("NullTarget: target = " + target_ + " region = " + region_ + "spans = " + spans_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static NullTarget buildFromXML(String elemName, 
                                        Attributes attrs) throws IOException {
    if (!elemName.equals("nullTarget")) {
      return (null);
    }
    
    String target = null;
    String region = null;    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("target")) {
          target = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("region")) {
          region = CharacterEntityMapper.unmapEntities(val, false);
        }
      }
    }
    
    if (target == null) {
      throw new IOException();
    }
    
    return (new NullTarget(target, region));
  }  
}
