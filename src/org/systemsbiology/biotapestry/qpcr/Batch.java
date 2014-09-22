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

package org.systemsbiology.biotapestry.qpcr;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;

/****************************************************************************
**
** This holds a batch of QPCR perturbation data
*/

class Batch implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  //  CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
   final static int NO_TIME = -1;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<Measurement> measurements_;
  private int time_;
  private String date_;
  private String invest_;
  private String batchKey_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  //  CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  Batch(BTState appState) {
    appState_ = appState;
    measurements_ = new ArrayList<Measurement>();
    time_ = NO_TIME;
    date_ = null;
    invest_ = null;
    batchKey_ = null;    
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  Batch(BTState appState, int time, String date, String invest) {
    appState_ = appState;
    measurements_ = new ArrayList<Measurement>();
    time_ = time;
    date_ = date;
    invest_ = invest;
    batchKey_ = null;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  Batch(BTState appState, int time, String date, String invest, String batchKey) {
    appState_ = appState;
    measurements_ = new ArrayList<Measurement>();
    time_ = time;
    date_ = date;
    invest_ = invest;
    batchKey_ = batchKey;
  }  

  /***************************************************************************
  **
  ** Copy Constructor
  */

   Batch(Batch other) {
    this.restore(other);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  //  METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Clone
  */

   public Batch clone() {
    try {
      Batch newVal = (Batch)super.clone();
      newVal.measurements_ = new ArrayList<Measurement>();
      Iterator<Measurement> omit = this.measurements_.iterator();
      while (omit.hasNext()) {
        newVal.measurements_.add(omit.next().clone());
      }
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }
  
  /***************************************************************************
  **
  ** match state of argument
  */

   void restore(Batch other) {
    this.measurements_ = new ArrayList<Measurement>();
    this.time_ = other.time_;
    this.date_ = other.date_;
    this.invest_ = other.invest_;
    this.batchKey_ = other.batchKey_;
    Iterator<Measurement> omit = other.measurements_.iterator();
    while (omit.hasNext()) {
      this.measurements_.add(new Measurement(omit.next()));
    }
  }
  
  /***************************************************************************
  **
  ** Add a measurement
  */
  
   void addMeasurement(Measurement meas) {
    measurements_.add(meas);
    return;
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the measurements
  */
  
   Iterator<Measurement> getMeasurements() {
    return (measurements_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the count of measurements
  */
  
   int getMeasurementCount() {
    return (measurements_.size());
  }
  
  /***************************************************************************
  **
  ** Drop all the measurements
  */
  
   void dropMeasurements() {
    measurements_.clear();
    return;
  }  

  /***************************************************************************
  **
  ** Set the date
  */
  
   void setDate(String date) {
    date_ = date;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the time
  */
  
   void setTime(int time) {
    time_ = time;
    return;
  }
  
  /***************************************************************************
  **
  ** Set the investigator(s)
  */
  
   void setInvestigators(String invest) {
    invest_ = invest;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the date (may be null if no date)
  */
  
   String getDate() {
    return (date_);
  }
  
  /***************************************************************************
  **
  ** Get the time (may be NO_TIME if no time)
  */
  
   int getTimeNumber() {
    return (time_);
  }
  
  /***************************************************************************
  **
  ** Get the display string
  **
  */
  
   String getTimeString() {
    if (time_ == NO_TIME) {
      return (null);
    }
    return (TimeAxisDefinition.getTimeDisplay(appState_, new Integer(time_), false, false));
  } 
  
  /***************************************************************************
  **
  ** Get the batch key (may be null)
  **
  */
  
   String getBatchKey() {
    return (batchKey_);
  }  
  
  /***************************************************************************
  **
  ** Set the batch key (optional)
  */
  
   void setBatchKey(String key) {
    batchKey_ = key;
    return;
  }    
 
  /***************************************************************************
  **
  ** Get the investigator(s) (may be null if none)
  */
  
   String getInvestigators() {
    return (invest_);
  }
  
  /***************************************************************************
  **
  ** Get the set of footnote numbers used by this batch
  */
  
   Set<String> getFootnoteNumbers() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Measurement> ms = getMeasurements();
    while (ms.hasNext()) {
      Measurement m = ms.next();
      List<String> mNotes = m.getFootnoteNumbers();
      retval.addAll(mNotes);
    }
    return (retval);
  }

  /***************************************************************************
  **
  ** Write the Batch to HTML
  */
  
   void writeHTML(PrintWriter out, Indenter ind, QpcrTablePublisher qtp, Integer batchTime) {
    Iterator<Measurement> mit = getMeasurements();
    while (mit.hasNext()) {
      Measurement m = mit.next();
      m.writeHTML(out, ind, qtp, batchTime);
      if (mit.hasNext()) {
        out.print(",");
        qtp.breakSpace();
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get the non-standard times for the batch 
  */
  
   void getNonStandardTimes(Set<Integer> nonStd, int stdMin, int stdMax) {
    Iterator<Measurement> ms = getMeasurements();
    while (ms.hasNext()) {
      Measurement m = ms.next();
      if (m.isNonStandardTime(stdMin, stdMax)) {
        nonStd.add(m.getTime());
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Answer if batch has a standard time measurement
  */
  
   boolean hasStandardTime(int stdMin, int stdMax) {
    Iterator<Measurement> ms = getMeasurements();
    while (ms.hasNext()) {
      Measurement m = ms.next();
      if (!m.isNonStandardTime(stdMin, stdMax)) {
        return (true);
      }
    }
    return (false);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  //  CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
   static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();

    retval.add("batch");  
    retval.add("nullBatch");      
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
   public String toString() {
    return ("Batch: measurements = " + measurements_);
  }

  /***************************************************************************
  **
  ** Answer if we are for a null batch
  ** 
  */
  
   static boolean isForNull(String elemName) {
    return (elemName.equals("nullBatch"));
  }  
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static Batch buildFromXML(BTState appState, String elemName, 
                             Attributes attrs) throws IOException {
    if (!elemName.equals("batch") && !elemName.equals("nullBatch")) {
      return (null);
    }
    
    String time = null;
    String date = null;
    String invest = null;
    String batchKey = null;
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("time")) {
          time = val;
        } else if (key.equals("date")) {
          date = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("invest")) {
          invest = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("key")) {
          batchKey = CharacterEntityMapper.unmapEntities(val, false);
        } 
      }
    }
    
    int timeVal = NO_TIME;
    if (time != null) {
      try {
        timeVal = Integer.parseInt(time);
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    
    if ((timeVal != NO_TIME) || (date != null) || (invest != null) || (batchKey != null)) {
      return (new Batch(appState, timeVal, date, invest, batchKey));
    } else {
      return (new Batch(appState));
    }
  }  
}
