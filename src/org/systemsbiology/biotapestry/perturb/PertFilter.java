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

import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.Vector;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;

/****************************************************************************
**
** This is used to filter results
**
*/

public class PertFilter implements Cloneable, PertFilterOpTarget {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  //
  // Categories:
  //
  
  public enum Cat {
    EXPERIMENT,
    SOURCE,
    SOURCE_NAME,
    SOURCE_OR_PROXY_NAME,
    PERT,
    TARGET,
    TIME,
    VALUE,
    INVEST,
    INVEST_LIST,
    EXP_CONTROL,
    EXP_CONDITION,
    MEASURE_SCALE,
    ANNOTATION,
    MEASURE_TECH,
  }
  
  public final static String INVEST_STR       = "Investigator"; 
  public final static String INVEST_LIST_STR  = "InvestigatorList";
 
  //
  // match types
  //
  
  public enum Match {
    STR_EQUALS,
    STR_CONTAINS,
    NUMB_EQ,
    NUMB_GTE,
    NUMB_LTE,
    NUMB_ABS_GTE,
    NUMB_ABS_LTE,
    RANGE_EQUALS,
    RANGE_OVERLAPS,
    ABOVE_THRESH,
    IS_SIGNIFICANT,
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private Cat category_;
  private Object value_;
  private Match matchType_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */  
  
  public PertFilter(Cat category, Match matchType, Object value) {    
    category_ = category;
    value_ = value;
    matchType_ = matchType;
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
  public PertFilter clone() {
    try {
      PertFilter newVal = (PertFilter)super.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }
  
  /***************************************************************************
  **
  ** Get the category
  */
  
  public Cat getCategory() {
    return (category_);
  }
  
  /***************************************************************************
  **
  ** Get the String value
  */
  
  public String getStringValue() {
    return ((String)value_);
  }
  
  /***************************************************************************
  **
  ** Get the double value
  */
  
  public double getDoubleValue() {
    return (((Double)value_).doubleValue());
  }
  
  /***************************************************************************
  **
  ** Get the int value
  */
  
  public int getIntValue() {
    return (((Integer)value_).intValue());
  }
  
  /***************************************************************************
  **
  ** Get the Int range value
  */
  
  public MinMax getIntRangeValue() {
    return ((MinMax)value_);
  }
   
  /***************************************************************************
  **
  ** Get the match Type
  */
  
  public Match getMatchType() {
    return (matchType_);
  }
  
  /***************************************************************************
  **
  ** Get the result
  */
  
  public SortedSet<String> getFilteredResult(SortedSet<String> input, SortedMap<String, ? extends PertFilterTarget> source, SourceSrc ss) {
    
    TreeSet<String> retval = new TreeSet<String>();
    Iterator<String> iit = input.iterator();
    while (iit.hasNext()) {
      String key = iit.next();
      PertFilterTarget pft = source.get(key);
      if (pft.matchesFilter(this, ss)) {
        retval.add(key);
      }
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get string
  */
  
  @Override
  public String toString() {
    return ("[pertFilt: " + category_ + ",t=" + matchType_ + ",v=" + value_ + "]");
  } 
  
 /***************************************************************************
  **
  ** Get the choices for controls
  */
  
  public static Vector<TrueObjChoiceContent> getMatchOptions(DataAccessContext dacx) { 
    TreeSet<TrueObjChoiceContent> sorted = new TreeSet<TrueObjChoiceContent>();
    sorted.add(getMatchOptionsChoice(dacx, Match.STR_EQUALS));
    sorted.add(getMatchOptionsChoice(dacx, Match.STR_CONTAINS));
    return (new Vector<TrueObjChoiceContent>(sorted));
  }  
  
  /***************************************************************************
  **
  ** Get the choice for a Match
  */
  
  public static TrueObjChoiceContent getMatchOptionsChoice(DataAccessContext dacx, Match choice) {
    if (choice == Match.STR_EQUALS) {
      return (new TrueObjChoiceContent(dacx.getRMan().getString("pertFilt.equals"), choice));
    } else if (choice == Match.STR_CONTAINS) {
      return (new TrueObjChoiceContent(dacx.getRMan().getString("pertFilt.contains"), choice));
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** map the category (incomplete...)
  */
  
  public static String mapCategory(Cat cat) {
    switch (cat) {
      case INVEST:
        return (INVEST_STR);      
      case INVEST_LIST:
        return (INVEST_LIST_STR);
      default:
        throw new IllegalArgumentException();
    }
  }
}
