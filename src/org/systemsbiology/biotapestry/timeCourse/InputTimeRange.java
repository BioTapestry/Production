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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

/****************************************************************************
**
** This is a Temporal Perturbation Range
*/

public class InputTimeRange implements Cloneable {
  
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
      RegionAndRange rar = other.ranges_.get(i);
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
  ** Clone
  **
  */  
  
  @Override
  public InputTimeRange clone() { 
    try {
      InputTimeRange newVal = (InputTimeRange)super.clone();
      newVal.ranges_ = new ArrayList<RegionAndRange>();
      int size = this.ranges_.size();
      for (int i = 0; i < size; i++) {
        RegionAndRange rar = this.ranges_.get(i);
        newVal.ranges_.add(rar.clone());
      }
      return (newVal);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  } 

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
  
  @Override
  public String toString() {
    return ("InputTimeRange: name_ = " + name_ + " ranges = " + ranges_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class InputTimeRangeWorker extends AbstractFactoryClient {
    
    private DataAccessContext dacx_;
    private RegionAndRange.RegionAndRangeWorker rarw_;
    
    public InputTimeRangeWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("inputTimeRange");
      rarw_ = new RegionAndRange.RegionAndRangeWorker(whiteboard);
      installWorker(rarw_, new MyRangeGlue());
    }
    
    public void installContext(DataAccessContext dacx) {
      dacx_ = dacx;
      rarw_.installContext(dacx_);
      return;
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("inputTimeRange")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.inputTimeRange = buildFromXML(elemName, attrs);
        retval = board.inputTimeRange;
      }
      return (retval);     
    }  
    
    private InputTimeRange buildFromXML(String elemName, Attributes attrs) throws IOException { 
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "inputTimeRange", "input", true);
      name = CharacterEntityMapper.unmapEntities(name, false);
      return (new InputTimeRange(name));
    }
  }
  
  public static class MyRangeGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      board.inputTimeRange.add(board.regionAndRange);
      return (null);
    }
  }
  
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
}
