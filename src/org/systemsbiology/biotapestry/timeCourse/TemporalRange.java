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
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;
import java.text.MessageFormat;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This holds temporal input range data for a gene
*/

public class TemporalRange {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String name_;
  private ArrayList<InputTimeRange> data_;
  private String note_;
  private boolean internalOnly_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public TemporalRange(String name, String note, boolean internalOnly) {
    name_ = name;
    note_ = note;
    internalOnly_ = internalOnly;
    data_ = new ArrayList<InputTimeRange>();
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

  public TemporalRange(TemporalRange other) {
    this.name_ = other.name_;
    this.note_ = other.note_;
    this.internalOnly_ = other.internalOnly_;
    this.data_ = new ArrayList<InputTimeRange>();
    int size = other.data_.size();
    for (int i = 0; i < size; i++) {
      InputTimeRange pert = other.data_.get(i);
      this.data_.add(new InputTimeRange(pert));
    }
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public TemporalRange(String name, String note, String internalOnly) throws IOException {
    name_ = name;
    note_ = note;
    
    if (internalOnly != null) {
      internalOnly = internalOnly.trim();
      if (internalOnly.equalsIgnoreCase("no")) {
        internalOnly_ = false;
      } else if (internalOnly.equalsIgnoreCase("yes")) {
        internalOnly_ = true;
      } else {
        throw new IOException();
      }
    } else {
      internalOnly_ = false;
    }
    data_ = new ArrayList<InputTimeRange>();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the name
  */
  
  public String getName() {
    return (name_);
  }
  
  /***************************************************************************
  **
  ** Set the name
  */
  
  public void setName(String name) {
    name_ = name;
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
  ** Answer if the data is for internal calculations only
  */
  
  public boolean isInternalOnly() {
    return (internalOnly_);
  }
  
  /***************************************************************************
  **
  ** Set if data is only to be used for data calculation, not for display
  */
  
  public void setInternalOnly(boolean state) {
    internalOnly_ = state;
    return;
  }   

  /***************************************************************************
  **
  ** Set note
  */
  
  public void setNote(String note) {
    note_ = note;
    return;
  }
  
  /***************************************************************************
  **
  ** Add a time range entry
  */
  
  public void addTimeRange(InputTimeRange pert) {
    data_.add(pert);
    return;
  }

  /***************************************************************************
  **
  ** Drop all time range entries
  */
  
  public void dropTimeRanges() {
    data_.clear();
    return;
  }  
 
  /***************************************************************************
  **
  ** Get an iterator over the time ranges
  */
  
  public Iterator<InputTimeRange> getTimeRanges() {
    return (data_.iterator());
  }

  /***************************************************************************
  **
  ** Get the given time range
  */
  
  public InputTimeRange getTimeRange(String name) {
    Iterator<InputTimeRange> pit = getTimeRanges();
    while (pit.hasNext()) {
      InputTimeRange itr = pit.next();
      if (itr.getName().equals(name)) {
        return (itr);
      }
    }
    return (null);
  }  
    
  /***************************************************************************
  **
  ** Add to the set of "interesting times"
  **
  */
  
  public void getInterestingTimes(Set<Integer> interest) {
    Iterator<InputTimeRange> perts = getTimeRanges();
    while (perts.hasNext()) {
      InputTimeRange pert = perts.next();
      pert.getInterestingTimes(interest);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Add to the set of regions
  **
  */
  
  public void getAllRegions(Set<String> interest) {
    Iterator<InputTimeRange> perts = getTimeRanges();
    while (perts.hasNext()) {
      InputTimeRange pert = perts.next();
      pert.getAllRegions(interest);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Write the Input Range Gene to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<temporalRange name=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    if (note_ != null) {
      out.print("\" note=\"");
      out.print(CharacterEntityMapper.mapEntities(note_, false));
    }
    if (internalOnly_) {
      out.print("\" internalOnly=\"yes");
    }
    if (data_.size() > 0) {
      out.println("\">");          
      ind.up(); 
      Iterator<InputTimeRange> perts = getTimeRanges();
      while (perts.hasNext()) {
        InputTimeRange pert = perts.next();
        pert.writeXML(out, ind);
      }
      ind.down().indent();       
      out.println("</temporalRange>");
    } else {
      out.println("\"/>");
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get an HTML inputs table suitable for display.
  */
  
  public void getInputsTable(BTState appState, PrintWriter out, Set<String> srcIDs) {

    if (data_.size() == 0) {
      return;
    }
 
    out.println("<table border=\"1\" width=\"75%\" cellpadding=\"7\" cellspacing=\"0\">");  
    // Heading
    
    out.println("<tr>"); 
    out.print("<td width=\"20%\">");
    out.print("<p align=\"center\">");    
    out.print("Gene");
    out.println("</p>");    
    out.println("</td>");
    out.println("<td width=\"20%\">");
    out.print("<p align=\"center\">");
    out.print("Input");
    out.println("</p>");      
    out.println("</td>");
    out.println("<td width=\"30%\">");    
    out.print("<p align=\"center\">");
    out.print("Time");
    out.println("</p>");      
    out.println("</td>");
    out.println("<td width=\"30%\">");    
    out.print("<p align=\"center\">");
    out.print("Notes");
    out.println("</p>");      
    out.println("</td>");    
    out.println("</tr>"); 
    
    //
    // Figure out if we accept numbers or names for the stages:
    //
    
    TimeAxisDefinition tad = appState.getDB().getTimeAxisDefinition();
    boolean namedStages = tad.haveNamedStages();
    String displayUnits = tad.unitDisplayAbbrev();
    ResourceManager rMan = appState.getRMan();

    boolean isFirst = true;
    Iterator<InputTimeRange> perts = getTimeRanges();
    while (perts.hasNext()) {
      InputTimeRange pert = perts.next();
      String rangeName = pert.getName();
      if ((srcIDs != null) && !DataUtil.containsKey(srcIDs, rangeName)) {
        continue;
      }
      out.println("<tr>");
      if (isFirst) {
        out.print("<td rowspan=\"");
        out.print(data_.size());    
        out.println("\">");
        out.println(name_);
        out.println("</td>");
        isFirst = false;
      }
      out.println("<td>");
      out.println(rangeName);
      out.println("</td>");
    
      //
      // Times and (parenthetical) regions:
      //
      
      out.println("<td>");
      out.println("<table border=\"0\" width=\"100%\" cellspacing=\"0\">");  
      out.println("<tr>");
      Iterator<RegionAndRange> rit = pert.getRanges();
  
      while (rit.hasNext()) {
        RegionAndRange rar = rit.next();
        out.println("<td ");
        int sign = rar.getSign();
        if (sign == RegionAndRange.PROMOTER) {
          out.print(" bgcolor=\"#66EE66\">");
        } else { 
          out.print(" bgcolor=\"#EE6666\">");
        }
        getRangeLine(out, srcIDs, rar, tad, namedStages, displayUnits, rMan); 
        out.println("</td>");
        out.println("</tr>");
      }
      out.println("</table>");  
      out.println("</td>");
      
      
      //
      // Notes:
      //
      
      out.println("<td>");
      out.println("<table border=\"0\" width=\"100%\" cellspacing=\"0\">");  
      out.println("<tr>");
      rit = pert.getRanges();
  
      while (rit.hasNext()) {
        RegionAndRange rar = rit.next();
        out.println("<td>");
        String note = rar.getNote();
        if (note != null) {
          out.println(note);
        } else {
          out.println("&nbsp;");
        }
        out.println("</td>");
        out.println("</tr>");
      }
      out.println("</table>");  
      out.println("</td>");
      out.println("</tr>");
    }   
    out.println("</table>");  
    return;
  }
  
  /***************************************************************************
  **
  ** Get single range line for display
  */
  
  public void getRangeLine(PrintWriter out, Set<String> srcIDs, RegionAndRange rar,  TimeAxisDefinition tad, boolean namedStages, String displayUnits, ResourceManager rMan ) { 
    //
    // FIX ME: In endomesoderm, 0 hr used to be written as "M".  Not currently supported.
    //
    int min = rar.getMin();
    int max = rar.getMax();
    String minTime = (namedStages) ? tad.getNamedStageForIndex(min).name : Integer.toString(min);        
    if (min == max) {          
      String whichFormat = (tad.unitsAreASuffix()) ? "tempoRange.singleTime" : "tempoRange.singleTimePrefix";
      String display = MessageFormat.format(rMan.getString(whichFormat), 
                                            new Object[] {minTime, displayUnits});  

      out.print(display);
    } else {
      String maxTime = (namedStages) ? tad.getNamedStageForIndex(max).name : Integer.toString(max);
      String whichFormat = (tad.unitsAreASuffix()) ? "tempoRange.timeRange" : "tempoRange.timeRangePrefix";          
      String display = MessageFormat.format(rMan.getString(whichFormat), 
                                            new Object[] {minTime, maxTime, displayUnits});
      out.print(display);
    }
    String reg = rar.getRegion();
    if (reg != null) {
      out.print(" (");
      out.print(reg); 
      out.print(")");
    }
    return;
  }
   
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("TemporalRange: name = " + name_ + " perturbations = " + data_ 
            + " note = " + note_);
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
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("temporalRange");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static TemporalRange buildFromXML(String elemName, 
                                           Attributes attrs) throws IOException {
    if (!elemName.equals("temporalRange")) {
      return (null);
    }
    
    String name = null;
    String note = null;
    String internalOnly = null;
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("name")) {
          name = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("note")) {
          note = CharacterEntityMapper.unmapEntities(val, false);
        } else if (key.equals("internalOnly")) {
          internalOnly = val;
        }
      }
    }

    if (name == null) {
      throw new IOException();
    }
    
    return (new TemporalRange(name, note, internalOnly));
  }
}
