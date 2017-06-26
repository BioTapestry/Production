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
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.TreeMap;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;


/****************************************************************************
**
** This holds copy per embryo data for a target gene
*/

public class CopiesPerEmbryoGene implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  private final static int COUNTS_PER_ROW_ = 5;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String name_;
  private TreeMap<Integer, Double> data_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public CopiesPerEmbryoGene(CopiesPerEmbryoGene other) {
    this.name_ = other.name_;
    this.data_ = new TreeMap<Integer, Double>(other.data_);  // Contents are immutable!
  }   

  /***************************************************************************
  **
  ** Constructor
  */

  public CopiesPerEmbryoGene(String name) {
    name_ = name;
    data_ = new TreeMap<Integer, Double>();
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
  public CopiesPerEmbryoGene clone() {
    try {
      CopiesPerEmbryoGene newVal = (CopiesPerEmbryoGene)super.clone();
      newVal.data_ = new TreeMap<Integer, Double>(this.data_);
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }
  }  
  
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
  ** Add a count
  */
  
  public void addCount(Integer timeKey, double count) {
    data_.put(timeKey, new Double(count));
    return;
  }
  
  /***************************************************************************
  **
  ** Delete a count at the given time key
  */
  
  public void deleteExpression(Integer timeKey) {
    data_.remove(timeKey);
    return;
  }  
   
  /***************************************************************************
  **
  ** Get an iterator over the count keys
  */
  
  public Iterator<Integer> getCountTimes() {
    return (data_.keySet().iterator());
  }
  
  /***************************************************************************
  **
  ** Get a count.
  */
  
  public Double getCount(Integer timeKey) {
    return (data_.get(timeKey));
  }  
 
  /***************************************************************************
  **
  ** Get an HTML count table suitable for display.
  */
  
  public void getCountTable(PrintWriter out, TimeAxisDefinition tad) {
                                   
    Set<Integer> timeKeys = data_.keySet();
    int numTimes = timeKeys.size();
    int numRows = numTimes / COUNTS_PER_ROW_;
    if ((numTimes % COUNTS_PER_ROW_) > 0) {
      numRows++;
    }
    ArrayList<Integer> times = new ArrayList<Integer>(timeKeys);
    
    String el = " Expression Level (copies / embryo)";
        
    out.println("<table width=\"810\" border=\"1\" cellpadding=\"0\" cellspacing=\"0\">");  
    // Heading
    out.println("<tr>"); 
    out.println("<td colspan=\"5\" align=\"center\" valign=\"center\">");
    out.print(name_);
    out.print(":");
    out.println(el);
    out.println("</td>");
    int rowBase = 0;
    for (int i = 0; i < numRows; i++) {
      // Times
      out.println("<tr>");       
      for (int j = 0; j < COUNTS_PER_ROW_; j++) {
        int currEntry = rowBase + j;
        if (currEntry < numTimes) {
          Integer timeKey = times.get(currEntry);
          buildTimeCell(out, timeKey, tad);
        } else {
          buildEmptyCell(out);
        }
      }
      out.println("</tr>");       
        
      // Counts
         out.println("</tr>");
      for (int j = 0; j < COUNTS_PER_ROW_; j++) {
        int currEntry = rowBase + j;
        if (currEntry < numTimes) {
          Integer timeKey = times.get(currEntry);
          buildCountCell(out, timeKey);
        } else {
          buildEmptyCell(out);
        }
      }
      out.println("</tr>");    
      // Blanks
      if (i < (numRows - 1)) {
			  out.println("<tr height=\"10\" border=\"10\" >");
        out.println("<td colspan=\"5\">&nbsp;</td>");
        out.println("</tr>");       
      }
      rowBase += COUNTS_PER_ROW_;
    }
    out.println("</table>");   
    out.println("<p></p>");
    return;
  }   
    
  /***************************************************************************
  **
  ** Write the Copies per Embryo gene to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<copiesPerEmbryo gene=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
   
    if (data_.size() > 0) {
      out.println("\">");
      ind.up(); 
      Iterator<Integer> times = getCountTimes();
      while (times.hasNext()) {
        Integer time = times.next();
        writeXMLForTime(out, ind, time);
      }
      ind.down().indent();       
      out.println("</copiesPerEmbryo>");
    } else {
      out.println("\" />");
    }

    return;
  }
  
  /***************************************************************************
  **
  ** Write a time out to XML
  **
  */
  
  public void writeXMLForTime(PrintWriter out, Indenter ind, Integer timeVal) {
    ind.indent();    
    out.print("<copiesAtTime time=\"");
    out.print(timeVal);
    out.print("\" count=\"");
    out.print(getCount(timeVal));
    out.println("\" />");
    return;
  }  

  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("CopiesPerEmbryoGene: name = " + name_);
  }

  /***************************************************************************
  **
  ** Build a table cell for time data
  **
  */
  
  public void buildTimeCell(PrintWriter out, Integer timeObj, TimeAxisDefinition tad) {
    out.print("<td width=\"70\" align=\"center\" valign=\"center\"><b>");    
    out.print("<b>");
    String timeLabel = TimeAxisDefinition.getTimeDisplay(tad, timeObj, true, false);
    out.print(timeLabel);
    out.print("</b>");
    out.println("</td>");        
    return;
  }  

  /***************************************************************************
  **
  ** Build a table cell for count data
  **
  */
  
  public void buildCountCell(PrintWriter out, Integer key) {
    out.print("<td width=\"70\" align=\"center\" valign=\"center\">");
    Double count = getCount(key);
    out.print(count);
    out.println("</td>");        
    return;
  }
  
  /***************************************************************************
  **
  ** Build a table cell for no data
  **
  */
  
  public void buildEmptyCell(PrintWriter out) {
    out.print("<td width=\"70\" align=\"center\" valign=\"center\">");
    out.print("&nbsp;");
    out.println("</td>");        
    return;
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
    retval.add("copiesPerEmbryo");
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the cAtTime keyword
  **
  */
  
  public static String cAtTimeKeyword() {
    return ("copiesAtTime");
  }  
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static CopiesPerEmbryoGene buildFromXML(String elemName, 
                                                 Attributes attrs) throws IOException {
    if (!elemName.equals("copiesPerEmbryo")) {
      return (null);
    }
    
    String gene = null; 
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("gene")) {
          gene = CharacterEntityMapper.unmapEntities(val, false);
        }
      }
    }

    if (gene == null) {
      throw new IOException();
    }
    
    return (new CopiesPerEmbryoGene(gene));
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static int extractCpeTime(String elemName, 
                                   Attributes attrs) throws IOException {
    String timeVal = AttributeExtractor.extractAttribute(elemName, attrs, "copiesAtTime", "time", true);
    try {
      return (Integer.parseInt(timeVal));
    } catch (NumberFormatException nfe) {
      throw new IOException();
    }
    
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public static double extractCpeCount(String elemName, 
                                       Attributes attrs) throws IOException {
    String countVal = AttributeExtractor.extractAttribute(elemName, attrs, "copiesAtTime", "count", true);
    try {
      return (Double.parseDouble(countVal));
    } catch (NumberFormatException nfe) {
      throw new IOException();
    }
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
