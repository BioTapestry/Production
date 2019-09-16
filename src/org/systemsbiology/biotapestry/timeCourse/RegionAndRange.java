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

package org.systemsbiology.biotapestry.timeCourse;

import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;

/****************************************************************************
**
** This is a Temporal Perturbation Region
*/

public class RegionAndRange {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int PROMOTER  = 0;
  public static final int REPRESSOR = 1;
  
  // Boundary extrapolation strategies

  public static final int NO_STRATEGY_SPECIFIED = 0;  
  public static final int ON_AT_BOUNDARY        = 1;
  public static final int OFF_AT_BOUNDARY       = 2;
  public static final int NUM_STRATEGIES        = 3;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String region_;
  private String source_;
  private String note_;
  private int min_;
  private int max_;
  private int sign_;  
  private int startStrategy_;
  private int endStrategy_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public RegionAndRange(RegionAndRange other) {  
    this.region_ = other.region_;
    this.source_ = other.source_;
    this.note_ = other.note_;
    this.min_ = other.min_;
    this.max_ = other.max_;
    this.sign_ = other.sign_;  
    this.startStrategy_ = other.startStrategy_;
    this.endStrategy_ = other.endStrategy_;   
  }  

  /***************************************************************************
  **
  ** Constructor
  */

  public RegionAndRange(String inputTokens, String color, String note,
                         int startStrategy, int endStrategy) {  
    parseRange(inputTokens);
    sign_ = parseColorToken(color);
    note_ = note;
    source_ = null;
    startStrategy_ = startStrategy;
    endStrategy_ = endStrategy;    
  }

  /***************************************************************************
  **
  ** Constructor
  */

  public RegionAndRange(String inputTokens, String color, String note) {  
    this(inputTokens, color, note, NO_STRATEGY_SPECIFIED, NO_STRATEGY_SPECIFIED);
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public RegionAndRange(BTState appState, String region, String min, String max, 
                        String sign, String note, String source,
                        String startStrategy, String endStrategy) throws IOException {
    region_ = region;
    source_ = source;
    try {
      min_ = Integer.parseInt(min);
      max_ = Integer.parseInt(max);      
    } catch (NumberFormatException nfex) {
      throw new IOException();
    }

    TimeAxisDefinition tad = appState.getDB().getTimeAxisDefinition();
    if (!tad.spanIsOk(min_, max_)) {
      throw new IOException();
    }
    
    if (sign == null) {
      throw new IOException();
    } else {
      sign = sign.trim();
      if (sign.equalsIgnoreCase("promote")) {
        sign_ = PROMOTER;
      } else if (sign.equalsIgnoreCase("repress")) {
        sign_ = REPRESSOR;
      } else {
        throw new IOException();
      }
    }
    
    try {
      startStrategy_ = calculateStrategy(startStrategy, true);   
      endStrategy_ = calculateStrategy(endStrategy, false);
    } catch (IllegalArgumentException iae) {
      throw new IOException();
    }
  
    note_ = note;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public RegionAndRange(String region, int min, int max, 
                        int sign, String note, String source,
                        int startStrategy, int endStrategy) {  
    region_ = region;
    source_ = source;
    min_ = min;
    max_ = max;
    sign_ = sign;
    note_ = note;
    startStrategy_ = startStrategy;
    endStrategy_ = endStrategy;   
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public RegionAndRange(String region, int min, int max, 
                        int sign, String note, String source) {
    this(region, min, max, sign, note, source, 
         NO_STRATEGY_SPECIFIED, NO_STRATEGY_SPECIFIED);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the region
  **
  */
  
  public String getRegion() {
    return (region_);
  }

  /***************************************************************************
  **
  ** Set the region
  **
  */
  
  public void setRegion(String region) {
    region_ = region;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the restricted source
  **
  */
  
  public String getRestrictedSource() {
    return (source_);
  }
  
  /***************************************************************************
  **
  ** Set the restricted source
  **
  */
  
  public void setRestrictedSource(String region) {
    source_ = region;
    return;
  }   

  /***************************************************************************
  **
  ** Get the min
  **
  */
  
  public int getMin() {
    return (min_);
  }

  /***************************************************************************
  **
  ** Get the max
  **
  */
  
  public int getMax() {
    return (max_);
  } 

  /***************************************************************************
  **
  ** Get the max
  **
  */
  
  public boolean inconsistentStrategy() {
    if (min_ == max_) {
      if ((startStrategy_ != endStrategy_) || (startStrategy_ == OFF_AT_BOUNDARY)) {
        return (true);
      }
    } else if (((max_ - min_) == 1) &&
               (startStrategy_ == OFF_AT_BOUNDARY) &&
               (endStrategy_ == OFF_AT_BOUNDARY)) {
      return (true);
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Answer if the interaction is active at the given time:
  */
  
  public boolean isActive(int hour) {
    if ((min_ < hour) && (max_ > hour)) {
      return (true);
    }
    if ((min_ != hour) && (max_ != hour)) {     
      return (false);
    }
    //
    // Min or max equals, so we have to check for edge strategies!
    // If we are inconsistent, we ignore strategies.
    //
    if (inconsistentStrategy()) {     
      return (true);
    }
    
    if (hour == min_) {    
      return ((startStrategy_ == NO_STRATEGY_SPECIFIED) ||
              (startStrategy_ == ON_AT_BOUNDARY));
    } else if (hour == max_) {   
      return ((endStrategy_ == NO_STRATEGY_SPECIFIED) ||
              (endStrategy_ == ON_AT_BOUNDARY));     
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Set the min
  **
  */
  
  public void setMin(int min) {
    min_ = min;
    return;
  }

  /***************************************************************************
  **
  ** Set the max
  **
  */
  
  public void setMax(int max) {
    max_ = max;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the sign of the interaction
  */
  
  public int getSign() {
    return (sign_);
  }

  /***************************************************************************
  **
  ** Set the sign of the interaction
  */
  
  public void setSign(int sign) {
    sign_ = sign;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the note
  */
  
  public String getNote() {
    return (note_);
  }
  
  /***************************************************************************
  **
  ** Set the note
  */
  
  public void setNote(String note) {
    note_ = note;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the starting extrapolation strategy
  */
  
  public int getStartStrategy() {
    return (startStrategy_);
  }
  
  /***************************************************************************
  **
  ** Get the ending extrapolation strategy
  */
  
  public int getEndStrategy() {
    return (endStrategy_);
  }
  
  /***************************************************************************
  **
  ** Get the starting extrapolation strategy
  */
  
  public void setStartStrategy(int startStrategy) {
    startStrategy_ = startStrategy;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the ending extrapolation strategy
  */
  
  public void setEndStrategy(int endStrategy) {
    endStrategy_= endStrategy;
    return;
  }
  
  /***************************************************************************
  **
  ** Add to the set of "interesting times"
  **
  */
  
  public void getInterestingTimes(Set<Integer> interest) {
    interest.add(new Integer(min_));
    if (max_ != min_) {
      interest.add(new Integer(max_));
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Add to the set of regions
  **
  */
  
  public void getAllRegions(Set<String> interest) {
    if (region_ != null) {
      interest.add(region_);
    }
    // Fix for BT-11-25-09:1
    if (source_ != null) {
      interest.add(source_);
    }  
    return;
  }
  
  /***************************************************************************
  **
  ** Write the range to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<range");
    if (region_ != null) {
      out.print(" region=\"");
      out.print(CharacterEntityMapper.mapEntities(region_, false));
      out.print("\"");
    }
    if (source_ != null) {
      out.print(" source=\"");
      out.print(CharacterEntityMapper.mapEntities(source_, false));
      out.print("\"");
    }    
    out.print(" minTime=\"");
    out.print(min_);
    out.print("\" maxTime=\"");
    out.print(max_);
    out.print("\" sign=\"");
    switch (sign_) {
      case PROMOTER:
        out.print("promote");
        break;
      case REPRESSOR:
        out.print("repress");
        break;
    }
    String invert = mapStrategy(startStrategy_);
    if (invert != null) {
      out.print("\" starttype=\"");
      out.print(invert);
    }
    invert = mapStrategy(endStrategy_);
    if (invert != null) {
      out.print("\" endtype=\"");
      out.print(invert);
    }
    if (note_ != null) {
      out.print("\" note=\"");
      out.print(CharacterEntityMapper.mapEntities(note_, false));
    }
    out.println("\" />");    
    return;
  }

  /***************************************************************************
  **
  ** Answer if our sign matches the link sign
  */
  
  public boolean signMatch(int linkageSign) {

    if (sign_ == PROMOTER) {
      return ((linkageSign == Linkage.POSITIVE) || (linkageSign == Linkage.NONE));
    } else {
      return (linkageSign == Linkage.NEGATIVE);
    }
  }    

 /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("RegionAndRange: region_ = " + region_ + " min = " + min_ +
            " max = " + max_ + " sign = " + sign_ + " note = " + note_ +
            " source = " + source_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ///////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Figure out extrapolation strategy
  */
  
  @SuppressWarnings("unused")
  public static int calculateStrategy(String token, boolean isStart) {
  
    //
    // Defaults for start and stop:
    //
    
    if (token == null) {
      return (NO_STRATEGY_SPECIFIED);
    }
    
    if (token.equals("onAtBoundary")) {
      return (ON_AT_BOUNDARY);
    } else if (token.equals("offAtBoundary")) {
      return (OFF_AT_BOUNDARY);
    } else {
      throw new IllegalArgumentException();
    }
  }

  /***************************************************************************
  **
  ** Figure out extrapolation strategy
  */
  
  public static String mapStrategy(int val) {  
    switch (val) {
      case NO_STRATEGY_SPECIFIED:
        return (null);      
      case ON_AT_BOUNDARY:
        return ("onAtBoundary");
      case OFF_AT_BOUNDARY: 
        return ("offAtBoundary");
      default:
        throw new IllegalArgumentException();
    } 
  } 
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("range");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static RegionAndRange buildFromXML(BTState appState, String elemName, 
                                            Attributes attrs) throws IOException {
    if (!elemName.equals("range")) {
      return (null);
    }
    
    String region = null; 
    String min = null; 
    String max = null; 
    String sign = null;
    String note = null;
    String source = null;
    String startStrategy = null;
    String endStrategy = null;     
   
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("region")) {
          region = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("minTime")) {
          min = val;
        } else if (key.equals("maxTime")) {
          max = val;
        } else if (key.equals("sign")) {
          sign = val;          
        } else if (key.equals("note")) {
          note = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("source")) {
          source = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("starttype")) {
          startStrategy = val;
        } else if (key.equals("endtype")) {
          endStrategy = val;
        }
      }
    }
    
    if ((min == null) || (max == null) || (sign == null)) {
      throw new IOException();
    }
    
    return (new RegionAndRange(appState, region, min, max, sign, note, source, startStrategy, endStrategy));
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Parse the range string
  */
  
  private void parseRange(String rangeInput) {
    //
    // If we have two tokens, the one starting with a number or
    // an M[.-] is the time range, the other is a region code.
    //
    // Recognize M prefix
    Pattern mP = Pattern.compile("M[\\-\u2026].*");  // FIX ME ELLIPSIS
    //Pattern mP = Pattern.compile("M.*");
    // Recognize number prefix
    Pattern nP = Pattern.compile("\\d+.*");
    
    String[] result = rangeInput.split("\\s");
    if (result.length == 1) {
      parseTimes(result[0]);
    } else if (result.length == 2) {
      if (mP.matcher(result[0]).matches() || nP.matcher(result[0]).matches()) {        
        parseTimes(result[0]);
        region_ = result[1];
      } else {
        parseTimes(result[1]);
        region_ = result[0];
      }
    } else {
      System.err.println("Result length is " + result.length + " " + result);
      throw new IllegalArgumentException();
    }
    
    return;
  }

  /***************************************************************************
  **
  ** Parse the time string
  */
  
  private void parseTimes(String timeInput) {
    //
    // If we have a -, we have a min and a max:
    //
    Pattern dP = Pattern.compile("(.+)-(.+)");
    Matcher dPm = dP.matcher(timeInput);
    
    if (dPm.matches()) {
      min_ = parseTimeToken(dPm.group(1));
      max_ = parseTimeToken(dPm.group(2));
      return;
    }
    
    //
    // If we have a ellipsis or just a number, we 
    // have an implicit max:
    //
    Pattern eP = Pattern.compile("([0-9hrM]+)(\\u2026?\\.*)");
    //Pattern eP = Pattern.compile("([0-9hrM]*)(.)\\.*");    
    Matcher ePm = eP.matcher(timeInput);
    
    if (ePm.matches()) {
      min_ = parseTimeToken(ePm.group(1));
      if (ePm.group(2).trim().length() > 0) {
        max_ = 30;
      } else {
        max_ = min_;
      }
      return;
    }
    
    System.err.println(">" + timeInput + "<");    
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Parse the time token
  */
  
  private int parseTimeToken(String timeInput) {
    //
    // If we have an M, we are zero
    //
    if (timeInput.equals("M")) {
      return (0);
    }
    
    //
    // scrape off the Hour suffix and parse the remaining integer
    //
    
    Pattern p = Pattern.compile("([0-9]+)(hr?)?");
    Matcher pM = p.matcher(timeInput);
    
    if (pM.matches()) {
      try {
        return (Integer.parseInt(pM.group(1)));
      } catch (NumberFormatException nfex) {
        System.err.println(timeInput);
        throw new IllegalArgumentException();
      }
    }
    System.err.println(timeInput);    
    throw new IllegalArgumentException();
  }
  
  /***************************************************************************
  **
  ** Parse the color token
  */
  
  private int parseColorToken(String colorInput) {   
    if (colorInput.equals("FF0000")) {
      return (REPRESSOR);
    } else if (colorInput.equals("00FF00")) {
      return (PROMOTER);
    } else {
      System.err.println(colorInput);
      throw new IllegalArgumentException();
    }
  }
}
