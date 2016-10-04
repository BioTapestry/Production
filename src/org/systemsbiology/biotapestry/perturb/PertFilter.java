/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

import org.systemsbiology.biotapestry.app.BTState;
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
  
  public final static int EXPERIMENT           = 0;
  public final static int SOURCE               = 1;
  public final static int SOURCE_NAME          = 2;
  public final static int SOURCE_OR_PROXY_NAME = 3;
  public final static int PERT                 = 4;
  public final static int TARGET               = 5; 
  public final static int TIME                 = 6;
  public final static int VALUE                = 7; 
  public final static int INVEST               = 8; 
  public final static int INVEST_LIST          = 9;
  public final static int EXP_CONTROL          = 10;
  public final static int EXP_CONDITION        = 11;
  public final static int MEASURE_SCALE        = 12;
  public final static int ANNOTATION           = 13;
  public final static int MEASURE_TECH         = 14; 
  public final static int NUM_CAT              = 15;   
  
  public final static String INVEST_STR       = "Investigator"; 
  public final static String INVEST_LIST_STR  = "InvestigatorList";
 
  //
  // match types
  //
  
  public final static int STR_EQUALS   = 0;
  public final static int STR_CONTAINS = 1;
  public final static int NUMB_EQ      = 2;
  public final static int NUMB_GTE     = 3; 
  public final static int NUMB_LTE     = 4; 
  public final static int NUMB_ABS_GTE = 5; 
  public final static int NUMB_ABS_LTE = 6; 
  public final static int RANGE_EQUALS   = 7; 
  public final static int RANGE_OVERLAPS = 8; 
  public final static int ABOVE_THRESH   = 9; 
  public final static int IS_SIGNIFICANT = 10;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private int category_;
  private Object value_;
  private int matchType_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */  
  
  public PertFilter(int category, int matchType, Object value) {    
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
  
  public int getCategory() {
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
  
  public int getMatchType() {
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
  
  public String toString() {
    return ("[pertFilt: " + category_ + ",t=" + matchType_ + ",v=" + value_ + "]");
  } 
  
 /***************************************************************************
  **
  ** Get the choices for controls
  */
  
  public static Vector<TrueObjChoiceContent> getMatchOptions(BTState appState) { 
    TreeSet<TrueObjChoiceContent> sorted = new TreeSet<TrueObjChoiceContent>();
    sorted.add(getMatchOptionsChoice(appState, STR_EQUALS));
    sorted.add(getMatchOptionsChoice(appState, STR_CONTAINS));
    return (new Vector<TrueObjChoiceContent>(sorted));
  }  
  
  /***************************************************************************
  **
  ** Get the choice for a Match
  */
  
  public static TrueObjChoiceContent getMatchOptionsChoice(BTState appState, int choice) {
    if (choice == STR_EQUALS) {
      return (new TrueObjChoiceContent(appState.getRMan().getString("pertFilt.equals"), new Integer(choice)));
    } else if (choice == STR_CONTAINS) {
      return (new TrueObjChoiceContent(appState.getRMan().getString("pertFilt.contains"), new Integer(choice)));
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  /***************************************************************************
  **
  ** map the category (incomplete...)
  */
  
  public static String mapCategory(int cat) {
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
