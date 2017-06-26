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

package org.systemsbiology.biotapestry.db;

import java.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import java.io.PrintWriter;

import org.xml.sax.Attributes;  
 
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Definition of Time Axis
*/

public class TimeAxisDefinition implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  private static final int SECONDS_         = 0;
  private static final int MINUTES_         = 1;
  private static final int HOURS_           = 2;
  private static final int DAYS_            = 3;
  private static final int WEEKS_           = 4;
  private static final int MONTHS_          = 5;
  private static final int YEARS_           = 6;
  private static final int NUMBERED_STAGES_ = 7;
  private static final int NAMED_STAGES_    = 8;
  private static final int NUM_UNIT_TYPES_  = 9;
  
  private static final String SECONDS_TAG_         = "seconds";
  private static final String MINUTES_TAG_         = "minutes";
  private static final String HOURS_TAG_           = "hours";
  private static final String DAYS_TAG_            = "days";
  private static final String WEEKS_TAG_           = "weeks";
  private static final String MONTHS_TAG_          = "months";
  private static final String YEARS_TAG_           = "years";
  private static final String NUMBERED_STAGES_TAG_ = "numbered";
  private static final String NAMED_STAGES_TAG_    = "named";
  
  private static final int DEFAULT_SPAN_MIN_ = 0;
  
  private static final int DEFAULT_SPAN_MAX_SECONDS_         = 60;
  private static final int DEFAULT_SPAN_MAX_MINUTES_         = 60;
  private static final int DEFAULT_SPAN_MAX_HOURS_           = 24;  
  private static final int DEFAULT_SPAN_MAX_DAYS_            = 7;
  private static final int DEFAULT_SPAN_MAX_WEEKS_           = 4;
  private static final int DEFAULT_SPAN_MAX_MONTHS_          = 12;
  private static final int DEFAULT_SPAN_MAX_YEARS_           = 10;
  private static final int DEFAULT_SPAN_MAX_NUMBERED_        = 10;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int INVALID_STAGE_NAME = -1; 
  public static final int FIRST_STAGE        = 0; 
  
  public static final int LEGACY_UNITS       = HOURS_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private boolean initialized_;
  private int units_;
  private String userUnits_;
  private String userUnitAbbrev_;
  private boolean userUnitIsSuffix_;
  private ArrayList<NamedStage> namedStages_;
  private ResourceManager rMan_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public TimeAxisDefinition(ResourceManager rMan) {
    rMan_ = rMan;
    initialized_ = false;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Equals
  */  
  
  
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof TimeAxisDefinition)) {
      return (false);
    }
 
    TimeAxisDefinition otherTAD = (TimeAxisDefinition)other;
  
    if (this.initialized_ != otherTAD.initialized_) {
      return (false);
    }
    if (this.units_ != otherTAD.units_) {
      return (false);
    }  
    if (!DataUtil.stringsEqual(this.userUnits_, otherTAD.userUnits_)) {
      return (false);
    }
    if (!DataUtil.stringsEqual(this.userUnitAbbrev_, otherTAD.userUnitAbbrev_)) {
      return (false);
    }
    if (this.userUnitIsSuffix_ != otherTAD.userUnitIsSuffix_) {
      return (false);
    }
    if (!DataUtil.objsEqual(this.namedStages_, otherTAD.namedStages_)) {
      return (false);
    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public TimeAxisDefinition clone() {
    try {
      TimeAxisDefinition retval = (TimeAxisDefinition)super.clone();
      if (this.namedStages_ == null) {
        return (retval);
      }
      retval.namedStages_ = new ArrayList<NamedStage>();
      int numStages = this.namedStages_.size();
      for (int i = 0; i < numStages; i++) {
        retval.namedStages_.add(this.namedStages_.get(i).clone());
      }
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }

  /***************************************************************************
  **
  ** Default
  */
  
  public TimeAxisDefinition setToDefault() {  
    units_ = HOURS_;
    userUnits_ = null;
    userUnitAbbrev_ = null;
    userUnitIsSuffix_ = true; 
    namedStages_ = null;
    initialized_ = true;
    return (this);
  }  
  
  /***************************************************************************
  **
  ** Legacy: Used for old I/O
  */
  
  public TimeAxisDefinition setToLegacy() {
    return (setToDefault());  // same as default for the moment
  }   

  /***************************************************************************
  **
  ** Set it
  */
  
  public void setDefinition(int units, String userUnits, String userUnitAbbrev, boolean isSuffix, List<NamedStage> namedStages) {  
    units_ = units;
    if ((units_ == NAMED_STAGES_) || (units_ == NUMBERED_STAGES_)) {
      userUnits_ = (userUnits == null) ? "" : userUnits;
      userUnitAbbrev_ = (userUnitAbbrev == null) ? userUnits_ : userUnitAbbrev;
      userUnitIsSuffix_ = isSuffix;
    } else {
      userUnits_ = null;
      userUnitAbbrev_ = null;
      userUnitIsSuffix_ = true;
    }
    if (units_ == NAMED_STAGES_) {
      if (namedStages != null) {
        if (namedStages.isEmpty()) {
          throw new IllegalArgumentException();
        } else {
          namedStages_ = new ArrayList<NamedStage>(namedStages);
        }
      } else {
        throw new IllegalArgumentException();
      }
    } else {
      namedStages_ = null;
    }
    initialized_ = true;
    return;
  }   

  /***************************************************************************
  **
  ** Used for IO only
  */
  
  public void startDefinition(int units, String userUnits, String userUnitAbbrev, boolean isSuffix) throws IOException {  
    units_ = units;

    if ((units_ != NAMED_STAGES_) && (units_ != NUMBERED_STAGES_)) {
      initialized_ = true;
      userUnitIsSuffix_ = true;
      if (userUnits_ != null) {
        throw new IOException();
      }
      return;
    }
    userUnits_ = userUnits;   
    userUnitAbbrev_ = (userUnitAbbrev == null) ? userUnits : userUnitAbbrev;
    userUnitIsSuffix_ = isSuffix;
    if (units_ == NUMBERED_STAGES_) {
      initialized_ = true; 
    } else {
      namedStages_ = new ArrayList<NamedStage>();
      initialized_ = false;
    }
    return;
  }     
  
  /***************************************************************************
  **
  ** Used for IO only
  */
  
  public void addAStage(NamedStage stage) throws IOException {
    if (namedStages_.size() > 0) {
      NamedStage lastStage = namedStages_.get(namedStages_.size() - 1);
      if (stage.order != (lastStage.order + 1)) {
        throw new IOException();
      }
    }
    namedStages_.add(stage);
    initialized_ = true;
    return;
  }     
  
  /***************************************************************************
  **
  ** Answer if initialized
  */
  
  public boolean isInitialized() {
    return (initialized_);
  }    
 
  /***************************************************************************
  **
  ** Get the selected units
  */
  
  public int getUnits() {
    if (!initialized_) {
      throw new IllegalStateException();
    }
    return (units_);
  }  

  /***************************************************************************
  **
  ** Get default time span; depends on units.
  */
  
  public MinMax getDefaultTimeSpan() {
    if (!initialized_) {
      throw new IllegalStateException();
    }
    switch (units_) {
      case SECONDS_:
        return (new MinMax(DEFAULT_SPAN_MIN_, DEFAULT_SPAN_MAX_SECONDS_));
      case MINUTES_:
        return (new MinMax(DEFAULT_SPAN_MIN_, DEFAULT_SPAN_MAX_MINUTES_));
      case HOURS_:
        return (new MinMax(DEFAULT_SPAN_MIN_, DEFAULT_SPAN_MAX_HOURS_));
      case DAYS_:
        return (new MinMax(DEFAULT_SPAN_MIN_, DEFAULT_SPAN_MAX_DAYS_));
      case WEEKS_:
        return (new MinMax(DEFAULT_SPAN_MIN_, DEFAULT_SPAN_MAX_WEEKS_));
      case MONTHS_:
        return (new MinMax(DEFAULT_SPAN_MIN_, DEFAULT_SPAN_MAX_MONTHS_));
      case YEARS_:
        return (new MinMax(DEFAULT_SPAN_MIN_, DEFAULT_SPAN_MAX_YEARS_));
      case NUMBERED_STAGES_:
        return (new MinMax(DEFAULT_SPAN_MIN_, DEFAULT_SPAN_MAX_NUMBERED_));
      case NAMED_STAGES_:
        int max = namedStages_.size() - 1;
        return ((max == DEFAULT_SPAN_MIN_) ? new MinMax(DEFAULT_SPAN_MIN_) 
                                           : new MinMax(DEFAULT_SPAN_MIN_, max));
      default:
        throw new IllegalArgumentException();
    }    
  }  
  
  /***************************************************************************
  **
  ** Get a localized display string for units, including custom unit if applicable
  */
  
  public String unitDisplayString() {   
    if (haveCustomUnits()) {
      return (userUnits_);
    } else {
      return (displayStringForUnit(rMan_, units_));
    }
  }
  
  /***************************************************************************
  **
  ** Get a localized abbrev string for units, including custom unit if applicable
  */
  
  public String unitDisplayAbbrev() {   
    if (haveCustomUnits()) {
      return (userUnitAbbrev_);
    } else {
      return (abbrevStringForUnit(rMan_, units_));
    }
  } 
  
  /***************************************************************************
  **
  ** Answer if the units are a suffix (vs. a prefix) e.g. "Cleavage 6" versus "D1 phase"
  */
  
  public boolean unitsAreASuffix() {   
    if (haveCustomUnits()) {
      return (userUnitIsSuffix_);
    } else {
      return (true);  // Stock times are always a suffix
    }
  }   
  
  /***************************************************************************
  **
  ** Answer if we have named stages
  */
  
  public boolean haveNamedStages() {
    return (initialized_ && (namedStages_ != null));
  }
  
  /***************************************************************************
  **
  ** Answer if we have custom units
  */
  
  public boolean haveCustomUnits() {
    return (initialized_ && wantsCustomUnits(units_));
  }

  /***************************************************************************
  **
  ** Get the named stages
  */
  
  public List<NamedStage> getNamedStages() {
    if ((!initialized_) || (units_ != NAMED_STAGES_)) {
      throw new IllegalStateException();
    }
    return (namedStages_);
  } 

  /***************************************************************************
  **
  ** Answer if we have the named stage for the given index
  */
  
  public boolean haveNamedStageForIndex(int index) {
    if ((!initialized_) || (units_ != NAMED_STAGES_)) {
      throw new IllegalStateException();
    }
    if ((index < 0) || (index >= namedStages_.size())) {
      return (false);
    }
    return (namedStages_.get(index) != null);
  }  
  
  /***************************************************************************
  **
  ** Get the named stage for the given index
  */
  
  public NamedStage getNamedStageForIndex(int index) {
    if ((!initialized_) || (units_ != NAMED_STAGES_)) {
      throw new IllegalStateException();
    }
    return (namedStages_.get(index));
  }
  
  /***************************************************************************
  **
  ** Get the index for the given named stage.  Returns INVALID_STAGE_NAME if no stage match
  */
  
  public int getIndexForNamedStage(String stageName) {
    if ((!initialized_) || (units_ != NAMED_STAGES_)) {
      throw new IllegalStateException();
    }
    stageName = stageName.trim();
    int numStages = namedStages_.size();
    for (int i = 0; i < numStages; i++) {
      NamedStage currStage = namedStages_.get(i);
      if (DataUtil.keysEqual(currStage.name, stageName) ||
          DataUtil.keysEqual(currStage.abbrev, stageName)) {
        return (i);
      }
    }
    return (INVALID_STAGE_NAME);
  }  
  
  /***************************************************************************
  **
  ** Get the user units
  */
  
  public String getUserUnitName() {
    if ((!initialized_) || ((units_ != NAMED_STAGES_) && (units_ != NUMBERED_STAGES_))) {
      throw new IllegalStateException();
    }
    return (userUnits_);
  }
  
  /***************************************************************************
  **
  ** Get the user unit abbrev
  */
  
  public String getUserUnitAbbrev() {
    if ((!initialized_) || ((units_ != NAMED_STAGES_) && (units_ != NUMBERED_STAGES_))) {
      throw new IllegalStateException();
    }
    return (userUnitAbbrev_);
  }  

  /***************************************************************************
  **
  ** Helper for validity
  */

  public boolean spanIsOk(int min, int max) {      
    if (haveNamedStages()) {
      if (!haveNamedStageForIndex(min) || !haveNamedStageForIndex(max)) {
        return (false);
      }
    }
    if ((min < 0) || (max < 0) || (max < min)) {
      return (false);
    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Helper for validity
  */

  public boolean timeIsOk(int min) {      
    if (haveNamedStages()) {
      if (!haveNamedStageForIndex(min)) {
        return (false);
      }
    }
    if (min < 0) {
      return (false);
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Helper for validity with suffix!
  */
  
  public Integer timeStringParse(String timeStr) {
    int timeVal = Integer.MIN_VALUE;   
    if (timeStr == null) {
      return (null);
    } else {
      timeStr = timeStr.trim();
      if (haveNamedStages()) {
        timeVal = getIndexForNamedStage(timeStr);
        if (timeVal == INVALID_STAGE_NAME) {
          return (null);
        }
      } else {
        String displayUnitAbbrev = DataUtil.normKey(unitDisplayAbbrev()); 
        try {    
          int hIndex = DataUtil.normKey(timeStr).indexOf(displayUnitAbbrev);
          if (hIndex != -1) {
            timeStr = timeStr.substring(0, hIndex);
          }
          timeVal = Integer.parseInt(timeStr);
        } catch (NumberFormatException nfe) {
          return (null);
        }
      }
    }
    return (new Integer(timeVal));
  }

  /***************************************************************************
  **
  ** Write the axis definition to XML
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<timeAxisDefinition units=\"");
    out.print(mapUnitTypes(units_));
    if (userUnits_ != null) {
      out.print("\" customUnits=\"");
      out.print(CharacterEntityMapper.mapEntities(userUnits_, false));
      out.print("\" isSuffix=\"");
      out.print(Boolean.toString(userUnitIsSuffix_));
    }
    if (userUnitAbbrev_ != null) {
      out.print("\" customUnitAbbrev=\"");
      out.print(CharacterEntityMapper.mapEntities(userUnitAbbrev_, false));
    }
    if ((namedStages_ == null) || (namedStages_.isEmpty())) {
      out.println("\" />");
      return;
    }
    out.println("\" >");
    ind.up();
    int numStages = namedStages_.size();
    for (int i = 0; i < numStages; i++) {
      NamedStage stage = namedStages_.get(i);
      stage.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</timeAxisDefinition>");
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Answers if we want custom units
  */
 
  public static boolean wantsCustomUnits(int unitType) {
    return ((unitType == NUMBERED_STAGES_) || (unitType == NAMED_STAGES_));        
  }
  
  /***************************************************************************
  **
  ** Answer if we need named stages
  */
 
  public static boolean wantsNamedStages(int unitType) {
    return (unitType == NAMED_STAGES_);        
  }  
 
  /***************************************************************************
  **
  ** Map the unit types
  */
 
  public static String mapUnitTypes(int unitType) {
    switch (unitType) {
      case SECONDS_:
        return (SECONDS_TAG_);
      case MINUTES_:
        return (MINUTES_TAG_);        
      case HOURS_:
        return (HOURS_TAG_);
      case DAYS_:
        return (DAYS_TAG_);
      case WEEKS_:
        return (WEEKS_TAG_);        
      case MONTHS_:
        return (MONTHS_TAG_);
      case YEARS_:
        return (YEARS_TAG_);
      case NUMBERED_STAGES_:
        return (NUMBERED_STAGES_TAG_);        
      case NAMED_STAGES_:
        return (NAMED_STAGES_TAG_);        
      default:
        throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** map the unit type tags
  */

  public static int mapUnitTypeTag(String unitTypeTag) {  
    if (unitTypeTag.equalsIgnoreCase(SECONDS_TAG_)) {
      return (SECONDS_);
    } else if (unitTypeTag.equalsIgnoreCase(MINUTES_TAG_)) {
      return (MINUTES_);      
    } else if (unitTypeTag.equalsIgnoreCase(HOURS_TAG_)) {
      return (HOURS_);
    } else if (unitTypeTag.equalsIgnoreCase(DAYS_TAG_)) {
      return (DAYS_);      
    } else if (unitTypeTag.equalsIgnoreCase(WEEKS_TAG_)) {
      return (WEEKS_);
    } else if (unitTypeTag.equalsIgnoreCase(MONTHS_TAG_)) {
      return (MONTHS_);            
    } else if (unitTypeTag.equalsIgnoreCase(YEARS_TAG_)) {
      return (YEARS_);      
    } else if (unitTypeTag.equalsIgnoreCase(NUMBERED_STAGES_TAG_)) {
      return (NUMBERED_STAGES_);
    } else if (unitTypeTag.equalsIgnoreCase(NAMED_STAGES_TAG_)) {
      return (NAMED_STAGES_);
    } else {
      throw new IllegalArgumentException();
    }  
  }

  /***************************************************************************
  **
  ** Return possible unit values
  */
  
  public static Vector<ChoiceContent> getUnitTypeChoices(ResourceManager rMan) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_UNIT_TYPES_; i++) {
      retval.add(unitTypeForCombo(rMan, i));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent unitTypeForCombo(ResourceManager rMan, int unitType) {
    return (new ChoiceContent(displayStringForUnit(rMan, unitType), unitType));
  }
  
  /***************************************************************************
  **
  ** Get a localized display string
  */
  
  public static String displayStringForUnit(ResourceManager rMan, int unitType) {
    return (rMan.getString("timeAxis." + mapUnitTypes(unitType)));
  }
  
  /***************************************************************************
  **
  ** Get a localized display string
  */
  
  public static String abbrevStringForUnit(ResourceManager rMan, int unitType) {
    return (rMan.getString("timeAxis.abbrev_" + mapUnitTypes(unitType)));
  }  
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("timeAxisDefinition");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get stage keyword
  **
  */
  
  public static String getStageKeyword() {
    return ("timeStage");
  }

 /***************************************************************************
  **
  ** Get legacy default time span; depends on units.
  */
  
  public static MinMax getLegacyDefaultTimeSpan() {
    return (new MinMax(0, 24));
  }
 
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
  ** Handle creation from XML
  **
  */

  @SuppressWarnings("unused")
  public static TimeAxisDefinition buildFromXML(DataAccessContext dacx, String elemName, Attributes attrs) throws IOException {
    String unitTag = null;
    String custom = null;
    String customAbbrev = null;
    String isSuffix = null;

    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("units")) {
          unitTag = val;
        } else if (key.equals("customUnits")) {
          custom = val;          
        } else if (key.equals("isSuffix")) {
          isSuffix = val;           
        } else if (key.equals("customUnitAbbrev")) {
          customAbbrev = val;          
        } 
      }
    }
    
    if (unitTag == null) {
      throw new IOException();
    }
    
    int unit;
    try {
      unit = mapUnitTypeTag(unitTag);
    } catch (IllegalArgumentException ex) {
      throw new IOException();
    }
    
    boolean isSuffixVal = true; // doesn't matter if we have no value.
    if (isSuffix != null) {
      isSuffixVal = Boolean.valueOf(isSuffix).booleanValue();
    }
    
    if (custom != null) {
      custom = CharacterEntityMapper.unmapEntities(custom, false);
    }    
    
    if (customAbbrev != null) {
      customAbbrev = CharacterEntityMapper.unmapEntities(customAbbrev, false);
    }        
 
    TimeAxisDefinition tfd = new TimeAxisDefinition(dacx.getRMan());
    tfd.startDefinition(unit, custom, customAbbrev, isSuffixVal);
    return (tfd);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public static class NamedStage implements Cloneable {
    public String name;
    public String abbrev;
    public int order;
    
    public NamedStage(String name, String abbrev, int order) {
      this.name = name;
      this.abbrev = abbrev;
      this.order = order;
    }
    
    @Override
    public NamedStage clone() {
      try {
        return ((NamedStage)super.clone());
      } catch (CloneNotSupportedException cnse) {
        throw new IllegalStateException();
      }
    }
    
    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return (true);
      }
      if (other == null) {
        return (false);
      }
      if (!(other instanceof NamedStage)) {
        return (false);
      }
      NamedStage otherNS = (NamedStage)other;
    
      if (!DataUtil.stringsEqual(this.name, otherNS.name)) {
        return (false);
      }
      if (!DataUtil.stringsEqual(this.abbrev, otherNS.abbrev)) {
        return (false);
      }
         
      if (this.order != otherNS.order) {
        return (false);
      }
      return (true);
    } 

    
    public void writeXML(PrintWriter out, Indenter ind) {
      ind.indent();
      out.print("<timeStage order=\"");
      out.print(Integer.toString(order));
      out.print("\" name=\"");
      out.print(CharacterEntityMapper.mapEntities(name, false));
      out.print("\" abbrev=\"");
      out.print(CharacterEntityMapper.mapEntities(abbrev, false));
      out.println("\" />");
      return;
    }
    
    @SuppressWarnings("unused")
    public static NamedStage buildFromXML(String elemName, Attributes attrs) throws IOException {
      String name = null;
      String abbrev = null;
      String orderStr = null;
      if (attrs != null) {
        int count = attrs.getLength();
        for (int i = 0; i < count; i++) {
          String key = attrs.getQName(i);
          if (key == null) {
            continue;
          }
          String val = attrs.getValue(i);
          if (key.equals("name")) {
            name = val;
          } else if (key.equals("abbrev")) {
            abbrev = val;          
          } else if (key.equals("order")) {
            orderStr = val;
          } 
        }
      }

      if ((name == null) || (abbrev == null) || (orderStr == null)) {
        throw new IOException();
      }

      int order = -1;
      try {
        order = Integer.parseInt(orderStr); 
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }

      if (order < 0) {
        throw new IOException();
      }

      name = CharacterEntityMapper.unmapEntities(name, false);
      abbrev = CharacterEntityMapper.unmapEntities(abbrev, false);      
      return (new NamedStage(name, abbrev, order));
    }
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
