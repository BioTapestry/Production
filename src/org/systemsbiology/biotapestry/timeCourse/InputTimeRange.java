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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

/****************************************************************************
**
** This is a Temporal Perturbation Range
*/

public class InputTimeRange {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String name_;
  private ArrayList<RegionAndRange> ranges_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public InputTimeRange(String name) {
    name_ = name;
    ranges_ = new ArrayList<RegionAndRange>();
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public InputTimeRange(InputTimeRange other) {
    this.name_ = other.name_;
    this.ranges_ = new ArrayList<RegionAndRange>();
    int size = other.ranges_.size();
    for (int i = 0; i < size; i++) {
      RegionAndRange rar = (RegionAndRange)other.ranges_.get(i);
      this.ranges_.add(new RegionAndRange(rar));
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the name
  **
  */
  
  public String getName() {
    return (name_);
  }
  
  /***************************************************************************
  **
  ** Set the name
  **
  */
  
  public void setName(String name) {
    name_ = name;
    return;
  }  
  
  /***************************************************************************
  **
  ** Add a range entry
  */
  
  public void add(RegionAndRange range) {
    ranges_.add(range);
    return;
  }
  
  /***************************************************************************
  **
  ** Get the range count
  */
  
  public int getRangeCount() {
    return (ranges_.size());
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the ranges
  */
  
  public Iterator<RegionAndRange> getRanges() {
    return (ranges_.iterator());
  }

  /***************************************************************************
  **
  ** Add to the set of "interesting times"
  **
  */
  
  public void getInterestingTimes(Set<Integer> interest) {
    Iterator<RegionAndRange> rit = getRanges();
    while (rit.hasNext()) {
      RegionAndRange range = rit.next();
      range.getInterestingTimes(interest);
    }
    return;
  }
  
   /***************************************************************************
  **
  ** Add to the set of regions
  **
  */
  
  public void getAllRegions(Set<String> interest) {
    Iterator<RegionAndRange> rit = getRanges();
    while (rit.hasNext()) {
      RegionAndRange range = rit.next();
      range.getAllRegions(interest);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Write the expression to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<inputTimeRange input=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    out.println("\">");
    ind.up();    
    if (ranges_.size() > 0) {
      Iterator<RegionAndRange> rit = getRanges();
      while (rit.hasNext()) {
        RegionAndRange range = rit.next();
        range.writeXML(out, ind);
      }
    }
    ind.down().indent();       
    out.println("</inputTimeRange>");
    return;
  }

 /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("InputTimeRange: name_ = " + name_ + " ranges = " + ranges_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Split the range token into different regions
  */
  
  public static List<String> splitRanges(String rangeInput) {
    ArrayList<String> retval = new ArrayList<String>();
    //
    // If we have one or two tokens, we have one range.  If we have
    // a multiple of two, break them into twos, one pair for each range.
    //
    String[] result = rangeInput.split("\\s");
    if ((result.length == 1) || (result.length == 2)) {
      retval.add(rangeInput);
      return (retval);
    }
    if (result.length % 2 == 0) {
      for (int i = 0; i < result.length; i += 2) {
        retval.add(result[i] + " " + result[i + 1]);
      }
      return (retval);
    }
    throw new IllegalArgumentException();
  }
    
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("inputTimeRange");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static InputTimeRange buildFromXML(String elemName, 
                                            Attributes attrs) throws IOException {
    if (!elemName.equals("inputTimeRange")) {
      return (null);
    }
    
    String name = null;   

    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("input")) {
          name = CharacterEntityMapper.unmapEntities(val, false);
        }
      }
    }
    
    if (name == null) {
      throw new IOException();
    }
    
    return (new InputTimeRange(name));
  }
}
