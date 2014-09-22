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
import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.Splitter;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

/****************************************************************************
**
** This holds QPCR Measurements
*/

class Measurement implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  

   final static int ZERO    = 0;
   final static int PLUS    = 1;  
   final static int MINUS   = 2; 
   final static int UNKNOWN = 3; 
  
   final static int UNSIGNED_LINK = 0;
   final static int REPRESS_LINK  = 1;  
   final static int PROMOTE_LINK  = 2;   
   final static int NO_LINK       = 3;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String value_;
  private Integer time_;
  private Integer nonStdMaxTime_;  //Added after qpcr deprecation for display purposes
  private String notes_;
  private String control_;
  private Boolean isSignificant_;
  private String comments_;
  private String colorDisplay_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor.  Not doing validation; use class function first.
  */

  Measurement(BTState appState, String value, String time, String notes, String control, Boolean isSig, String comments) {
    appState_ = appState; 
    // Can be null while being created
    value_ = (value == null) ? null : value.trim().toUpperCase();
    time_ = timeConversion(appState_, time);
    nonStdMaxTime_ = null;
    notes_ = notes;
    control_ = control;
    isSignificant_ = isSig;
    comments_ = comments;
  }   
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  Measurement(Measurement other) {
    this.appState_ = other.appState_;
    this.value_ = other.value_;
    this.time_ = other.time_;
    this.nonStdMaxTime_ = other.nonStdMaxTime_;
    this.notes_ = other.notes_;
    this.control_ = other.control_;
    this.isSignificant_ = other.isSignificant_;
    this.comments_ = other.comments_;    
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Set a non-standard max time
  */

   void setNonStdMaxTime(String nonStdMax) {
     nonStdMaxTime_ = timeConversion(appState_, nonStdMax);
     return;
   }
 
  /***************************************************************************
  **
  ** Set the HTML display color
  */

   void setColorDisplay(String colorTag) {
     colorDisplay_ = colorTag;
     return;
   }
    
  /***************************************************************************
  **
  ** Clone
  */

   public Measurement clone() {
    try {
      Measurement newVal = (Measurement)super.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }
    
  /***************************************************************************
  **
  ** Answer if the measurement is non-standard
  */
  
   boolean isNonStandardTime(int stdMin, int stdMax) {
    if (time_ == null) {
      return (false);
    }
    int hrVal = time_.intValue();
    if (nonStdMaxTime_ == null) {
      return ((hrVal < stdMin) || (hrVal > stdMax));
    }
    int maxTimeVal = nonStdMaxTime_.intValue();
    return ((hrVal != stdMin) && (maxTimeVal != stdMax));  
  }  

  /***************************************************************************
  **
  ** Get the value.  Note this is the only way to access a sub-threshold
  ** numeric value!
  */
  
   String getValue() {
    return (value_);
  }
  
  /***************************************************************************
  **
  ** Get the significance tag.  May be null for legacy cases.
  */
  
   Boolean getIsSignificant() {
    return (isSignificant_);
  } 
  
  /***************************************************************************
  **
  ** Get the control
  */
  
   String getControl() {
    return (control_);
  } 
  
  /***************************************************************************
  **
  ** Get the comments
  */
  
   String getComment() {
    return (comments_);
  }   
  
  /***************************************************************************
  **
  ** Get the time (may be null)
  **
  */
  
   Integer getTime() {
    return (time_);
  }
  
  /***************************************************************************
  **
  ** Get the notes (may be null)
  **
  */
  
   String getNotes() {
    return (notes_);
  }
  
  /***************************************************************************
  **
  ** Get the list of footnote numbers used 
  */
  
   List<String> getFootnoteNumbers() {
    if (notes_ != null) {
      return (Splitter.stringBreak(notes_, ",", 0, true));
    } else {
      return (new ArrayList<String>());
    }    
  }

  /***************************************************************************
  **
  ** Write the measurement to HTML
  **
  */
  
   void writeHTML(PrintWriter out, Indenter ind, QpcrTablePublisher qtp, Integer batchTime) {
    boolean closeColor = false;
    if ((colorDisplay_ != null) && !colorDisplay_.equalsIgnoreCase("black")) {
      qtp.openColor(colorDisplay_);
      closeColor = true;
    }
    qtp.valueSignPrefix(value_);
    out.print(value_);
 
    if (closeColor) {
      qtp.closeColor();
    }    
    if (notes_ != null) {
      out.print("<sup>");
      out.print(notes_);
      out.print("</sup>");
    }
    if ((time_ != null) && ((batchTime == null) || !batchTime.equals(time_))) {
      out.print(" (");
      out.print(getTimeDisplay(true, true));
      out.print(")");
    }     
    return;
  }
  
  /***************************************************************************
  **
  ** Get the display string
  **
  */
  
   String getTimeDisplay(boolean showUnits, boolean abbreviate) {
    if (time_ == null) {
      return (null);
    }
    String minDisp = TimeAxisDefinition.getTimeDisplay(appState_, time_, showUnits, abbreviate);
    if (nonStdMaxTime_ == null) {
      return (minDisp);  
    } else {
      String maxDisp = TimeAxisDefinition.getTimeDisplay(appState_, nonStdMaxTime_, showUnits, abbreviate);
      return (minDisp + "-" + maxDisp);
    }  
  }  
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
   public String toString() {
    return ("Measurement: value = " + value_ + " time = " + time_ + " notes = " + notes_);
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
    retval.add("measurement");
    retval.add("nullMeasurement");    
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static Measurement buildFromXML(BTState appState, String elemName, 
                                   Attributes attrs, double legacyThresh) throws IOException {
    if (!elemName.equals("measurement") && !elemName.equals("nullMeasurement")) {
      return (null);
    }
    
    String value = null;
    String time = null;
    String notes = null;
    String control = null;
    String isSigString = null;
    String comments = null;    
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("value")) {
          value = val;
        } else if (key.equals("time")) {
          time = val;
        } else if (key.equals("notes")) {
          notes = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("control")) {
          control = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("isSig")) {
          isSigString = val;
        } else if (key.equals("comment")) {
          comments = CharacterEntityMapper.unmapEntities(val, false);
        }
      }
    }  
    
    if (value == null) {
      throw new IOException();
    }
    
    if (!isValidMeasurement(value)) {
      throw new IOException();
    }
    
    if (!isValidTime(appState, time)) {
      throw new IOException();
    }
    
    //
    // isSig will be null in the legacy case..
    //
    
    legacyThresh = Math.abs(legacyThresh);
    Boolean isSig = (isSigString != null) ? Boolean.valueOf(isSigString) 
                                          : new Boolean(!isBelowThreshold(value, null, -legacyThresh, legacyThresh)); 
    return (new Measurement(appState, value, time, notes, control, isSig, comments));
  }
  
  /***************************************************************************
  **
  ** Answers if the measurement is valid
  **
  */
  
   static boolean isValidMeasurement(String input) {
    if (input == null) {
      return (false);
    } 
    input = input.trim();
    if (input.equals("")) {
      return (false);
    }  
    if (input.toUpperCase().equals("NS")) {
      return (true);
    }
    if (isValidForMissingData(input)) {
      return (true);
    }
    
    try {
      Double.parseDouble(input);
    } catch (NumberFormatException nfe) {
      return (false); 
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answers if the hour is valid
  **
  */
  
   static boolean isValidTime(BTState appState, String input) {
    if (input == null) {
      return (true);
    } 
    input = input.trim();
    if (input.equals("")) {
      return (true);
    }
   
    TimeAxisDefinition tad = appState.getDB().getTimeAxisDefinition();
    if (tad.haveNamedStages()) {
      int stageNum = tad.getIndexForNamedStage(input);      
      return (stageNum != TimeAxisDefinition.INVALID_STAGE_NAME);
    }
    
    try {
      Integer.parseInt(input);
    } catch (NumberFormatException nfe) {
      return (false); 
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Map time strings to indices
  **
  */
  
   static Integer timeConversion(BTState appState, String input) {
    if (input == null) {
      return (null);
    } 
    input = input.trim();
    if (input.equals("")) {
      return (null);
    }
   
    TimeAxisDefinition tad = appState.getDB().getTimeAxisDefinition();
    if (tad.haveNamedStages()) {
      int stageNum = tad.getIndexForNamedStage(input);      
      if (stageNum == TimeAxisDefinition.INVALID_STAGE_NAME) {
        throw new IllegalArgumentException();
      }
      return (new Integer(stageNum));
    }
    
    try {
      return (Integer.decode(input));
    } catch (NumberFormatException nfe) {
      return (null); 
    }
  }  
  
  /***************************************************************************
  **
  ** Answer if the measurement represents missing data
  */
  
   static boolean isValidForMissingData(String val) {
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
  
   static boolean isBelowThreshold(String val, Boolean sigTag, double negThresh, double posThresh) {
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
  ** Convert the new CSV standard significance string
  */
  
   static Boolean convertSigInput(String isValid) {
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
  
   static boolean sigCSVInputOK(String isValid) {
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
}
