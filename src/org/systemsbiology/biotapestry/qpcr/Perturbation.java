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

package org.systemsbiology.biotapestry.qpcr;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MinMax;

import org.xml.sax.Attributes;

/****************************************************************************
**
** This holds QPCR perturbation data
*/

class Perturbation {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<TimeSpan> timeSpans_;
  private ArrayList<String> investigators_;
  private ArrayList<Source> sources_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

   Perturbation() {
    timeSpans_ = new ArrayList<TimeSpan>();
    investigators_ = new ArrayList<String>();
    sources_ = new ArrayList<Source>();    
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

   Perturbation(Perturbation other) {
    this.timeSpans_ = new ArrayList<TimeSpan>();
    Iterator<TimeSpan> otit = other.timeSpans_.iterator();
    while (otit.hasNext()) {
      this.timeSpans_.add(new TimeSpan(otit.next()));
    }
    //
    // Investigators are just strings:
    //
    this.investigators_ = new ArrayList<String>(other.investigators_);

    this.sources_ = new ArrayList<Source>();
    Iterator<Source> osit = other.sources_.iterator();
    while (osit.hasNext()) {
      this.sources_.add(new Source(osit.next()));
    }    
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Answer if the perturbation is active at the given time.  Ignores embedded
  ** non-standard times
  */
  
   boolean isActiveAtTime(MinMax range) {
    Iterator<TimeSpan> tsit = getTimeSpans();
    while (tsit.hasNext()) {
      TimeSpan t = tsit.next();  
      if (t.getMinMaxSpan().equals(range)) {
        if (t.hasStandardTime()) {
          return (true);
        }
      }
    }
    return (false);
  }

  /***************************************************************************
  **
  ** Answer if our set of sources matches the given set
  */
  
   boolean sourcesMatch(List<Source> otherSources) {
    if (otherSources.size() != sources_.size()) {
      return (false);
    }
    QPCRData.SourceComparator srcCmp = new QPCRData.SourceComparator();     
    TreeSet<Source> mySorted = new TreeSet<Source>(srcCmp);
    mySorted.addAll(sources_);
    TreeSet<Source> otherSorted = new TreeSet<Source>(srcCmp);
    otherSorted.addAll(otherSources);
    Iterator<Source> msit = mySorted.iterator();
    Iterator<Source> osit = otherSorted.iterator();    
    while (msit.hasNext()) {
      Source ms = msit.next();
      Source os = osit.next();
      if (srcCmp.compare(ms, os) != 0) {
        return (false);
      }
    }
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Add a source
  */
  
   void addSource(Source source) {
    sources_.add(source);
    return;
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the sources
  */
  
   Iterator<Source> getSources() {
    return (sources_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the source count
  */
  
   int getSourceCount() {
    return (sources_.size());
  }
  
  /***************************************************************************
  **
  ** Delete the source
  */
  
   void deleteSource(int index) {
    sources_.remove(index);
    return;
  }
  
  /***************************************************************************
  **
  ** Get a source
  */
  
   Source getSource(int index) {
    return (sources_.get(index));
  }

  /***************************************************************************
  **
  ** Get combined source name.  For single source perturbations, this is
  ** just the base name.  For multi-source, it is a concatenation.
  */
  
   String getCombinedSourceName(boolean useDisplayForSingle) {
    int num = sources_.size();
    if (num == 0) {
      return (null);
    } else if (num == 1) {
      if (useDisplayForSingle) {
        return (sources_.get(0).getDisplayValue());
      } else {
        return (sources_.get(0).getBaseType());       
      }
    } else {
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < num; i++) {
        Source src = sources_.get(i);
        buf.append(src.getDisplayValue());
        if (i < num - 1) {
          buf.append(" + ");
        }
      }
      return (buf.toString());
    }
  }  
  
  /***************************************************************************
  **
  ** Replace a source
  */
  
   void replaceSource(int index, Source src) {
    sources_.set(index, src);
    return;
  }
  
  /***************************************************************************
  **
  ** Add an investigator
  */
  
   void addInvestigator(String investigator) {
    investigators_.add(investigator);
    return;
  }
  
  /***************************************************************************
  **
  ** Add an investigator if it is new
  */
  
   void addInvestigatorIfNew(String investigator) {
    if (investigators_.contains(investigator)) {
      return;
    }
    investigators_.add(investigator);
    return;
  }  
  
  /***************************************************************************
  **
  ** Delete an investigator
  */
  
   void deleteInvestigator(int index) {
    investigators_.remove(index);
    return;
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the investigators
  */
  
   Iterator<String> getInvestigators() {
    return (investigators_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get an investigator
  */
  
   String getInvestigator(int index) {
    return (investigators_.get(index));
  }
  
  /***************************************************************************
  **
  ** Replace an investigator
  */
  
   void replaceInvestigator(int index, String invest) {
    investigators_.set(index, invest);
    return;
  }  
  /***************************************************************************
  **
  ** Get the investigator count
  */
  
   int getInvestigatorCount() {
    return (investigators_.size());
  }
  
  /***************************************************************************
  **
  ** Add a time span
  */
  
   void addTime(TimeSpan timeSpan) {
    timeSpans_.add(timeSpan);
    return;
  }
  
  /***************************************************************************
  **
  ** Drop a time span
  */
  
   void dropTime(MinMax span) {
    int num = timeSpans_.size();
    for (int i = 0; i < num; i++) {
      TimeSpan ts = timeSpans_.get(i);
      if (ts.getMinMaxSpan().equals(span)) {
        timeSpans_.remove(i);
        return;
      }
    }
    return;
  }  

  /***************************************************************************
  **
  ** Get an iterator over the time spans
  */
  
   Iterator<TimeSpan> getTimeSpans() {
    return (timeSpans_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get the time span for a certain time.  May be null.
  */
  
   TimeSpan getTimeSpan(MinMax span) {
    Iterator<TimeSpan> tsit = getTimeSpans();
    while (tsit.hasNext()) {
      TimeSpan ts = tsit.next();
      if (ts.getMinMaxSpan().equals(span)) {
        return (ts);
      }
    }
    return (null);
  }  
   
  /***************************************************************************
  **
  ** Get the set of footnote numbers used by this perturbation
  */
  
   Set<String> getFootnoteNumbers() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Source> srcs = getSources();
    while (srcs.hasNext()) {
      Source src = srcs.next();
      List<String> srcNotes = src.getFootnoteNumbers();
      retval.addAll(srcNotes);
    }
    Iterator<TimeSpan> times = getTimeSpans();
    while (times.hasNext()) {
      TimeSpan time = times.next();
      Set<String> notes = time.getFootnoteNumbers();
      retval.addAll(notes);
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Write the display string
  **
  */
  
   String displayString(boolean footnotes) {
    StringBuffer buf = new StringBuffer();
    Iterator<Source> sit = getSources();
    while (sit.hasNext()) {
      Source src = sit.next();
      buf.append(src.getDisplayValue());
      if (footnotes) {
        String notes = src.getNotes();
        if (notes != null) {
          buf.append(" [");
          buf.append(notes);
          buf.append("]");
        }
      }
      if (sit.hasNext()) {
        buf.append(" + ");
      }
    }
    return (buf.toString());
  }
 
  /***************************************************************************
  **
  ** Get ready to write to HTML
  **
  */
  
   void prepForHTML(List<SortedMap<String, Map<MinMax, TimeSpan>>> mapList, List<String> srcNames) {
     if (srcNames != null) {
       boolean gotIt = false;
       Iterator<Source> srcIt = sources_.iterator();
       while (srcIt.hasNext()) {
         Source src = srcIt.next();
         if (DataUtil.containsKey(srcNames, src.getBaseType())) {
           gotIt = true;
           break;
         }
       }
       if (!gotIt) {
         return;
       }
     }
     mapList.add(groupByInvestigator());
     return;
   }
   
   
  /***************************************************************************
  **
  ** Find out if we are going to be used:
  **
  */
  
  boolean matchesForHTML(List<String> srcNames) {  
   if (srcNames != null) {
      boolean gotIt = false;
      Iterator<Source> srcIt = sources_.iterator();
      while (srcIt.hasNext()) {
        Source src = srcIt.next();
        if (DataUtil.containsKey(srcNames, src.getBaseType())) {
          gotIt = true;
          break;
        }
      }
      if (!gotIt) {
        return (false);
      }
    }
    return (true);
  }
    
  /***************************************************************************
  **
  ** Write the Perturbation to HTML
  **
  */
  
  boolean writeHTML(PrintWriter out, Indenter ind, String geneTag, 
                    int numRows, List<MinMax> timeCols, QpcrTablePublisher qtp, 
                    SortedMap<String, Map<MinMax, TimeSpan>> gbi, 
                    boolean breakOutInvest, List<String> srcNames, TimeAxisDefinition tad) {
     
    if (!matchesForHTML(srcNames)) {
      return (false);
    }  
 
    ind.indent();    
    out.println("<tr valign=\"top\">");
    ind.up();
    
    // Print out the gene ID, if it is the first row:
    if (geneTag != null) {
      ind.indent();
      out.print("<td");
      if (numRows > 1) {
        out.print(" rowspan=\"");
        out.print(numRows);
        out.print("\" ");
      }
      out.println(">");
      ind.up().indent();
      qtp.paragraph(false);
      out.print("<i><b>");
      out.print(geneTag);
      out.println("</b></i></p>");
      ind.down().indent();
      out.println("</td>");
    }

    // Print out the sources:
    ind.indent();
    int srcRows = (gbi != null) ? gbi.keySet().size() : 1;
    out.print("<td");
    if (srcRows > 1) {
      out.print(" rowspan=\"");
      out.print(srcRows);
      out.print("\" ");
    }
    out.println(">");
    ind.up();
    Iterator<Source> srcs = getSources();   
    while (srcs.hasNext()) {
      Source src = srcs.next();
      src.writeHTML(out, ind, srcs.hasNext(), qtp, false);
    }      
    ind.down().indent();
    out.println("</td>");
    
    //
    // Even if investigators are broken out, we want multiple
    // time points in the span to be called out:
    //
    
    HashMap<MinMax, TimeSpan.SpanTimeProfile> timeProfs = new HashMap<MinMax, TimeSpan.SpanTimeProfile>();
    Iterator<TimeSpan> times = timeSpans_.iterator();
    while (times.hasNext()) {
      TimeSpan time = times.next();
      TimeSpan.SpanTimeProfile stp = time.getBatchTimeProfile();
      timeProfs.put(time.getMinMaxSpan(), stp);
    }
     
    if (breakOutInvest) {
      Iterator<String> gbiit = gbi.keySet().iterator();
      while (gbiit.hasNext()) {
        String invest = gbiit.next();
        Map<MinMax, TimeSpan> byInv = gbi.get(invest);
        writeOutTimeSpans(out, ind, qtp, timeCols, new ArrayList<TimeSpan>(byInv.values()), timeProfs, tad);
        if (invest.equals("WJRL_HACKASTIC_KLUDGE")) {
          writeOutInvestigators(out, ind, qtp, null, 0);
        } else {
          List<String> investList = Perturbation.unformatInvestigators(invest);
          writeOutInvestigators(out, ind, qtp, investList.iterator(), investList.size());
        }
        ind.down().indent();       
        out.println("</tr>");
      }
    } else {
      // Crank through time spans, output results if needed:
      writeOutTimeSpans(out, ind, qtp, timeCols, timeSpans_, timeProfs, tad);  

      // Output the investigators column:
      Iterator<String> iit = getInvestigators();
      int icount = investigators_.size();
      writeOutInvestigators(out, ind, qtp, iit, icount);
      ind.down().indent();       
      out.println("</tr>");
    }
    return (true);
  }  

  
  /***************************************************************************
  **
  ** Output time spans
  **
  */
  
  void writeOutTimeSpans(PrintWriter out, Indenter ind, QpcrTablePublisher qtp, 
                         List<MinMax> timeCols, List<TimeSpan> timeSpans, 
                         Map<MinMax, TimeSpan.SpanTimeProfile> timeProfs, TimeAxisDefinition tad) {  
    // Crank through time spans, output results if needed:
    Iterator<MinMax> tcit= timeCols.iterator();
    while (tcit.hasNext()) {
      MinMax tcol = tcit.next();
      ind.indent();
      out.println("<td>");
      ind.up();      
      Iterator<TimeSpan> times = timeSpans.iterator();
      boolean haveMatch = false;
      while (times.hasNext()) {
        TimeSpan time = times.next();
        MinMax mms = time.getMinMaxSpan();
        if (mms.equals(tcol)) {
          TimeSpan.SpanTimeProfile stp = timeProfs.get(mms);
          time.writeHTML(out, ind, qtp, stp, tad);
          haveMatch = true;
          break;
        }
      }
      if (!haveMatch) {  // Print out a blank box
        ind.indent();
        out.println("<p><br></p>");
      }
      ind.down().indent();
      out.println("</td>");
    }
    return;
  }
    
  /***************************************************************************
  **
  ** Write out the investigators
  **
  */
  
  void writeOutInvestigators(PrintWriter out, Indenter ind, QpcrTablePublisher qtp, Iterator<String> iit, int icount) {
    ind.indent();
    out.println("<td>");
    ind.up();
  
    if (icount == 0) {
      out.println("<p>&nbsp;</p>");
    } else {
      while (iit.hasNext()) {
        String inv = iit.next();
        ind.indent();
        if (icount > 0) {
          qtp.paragraph(false);
        } else {
          out.print("<p>");
        }
        out.print(inv);
        icount--;
        if (icount == 1) {
          out.print(" &amp;");
        } else if (icount > 0) {
          out.print(",");
        }
        out.println("</p>");
      }
    }
    ind.down().indent();
    out.println("</td>"); 
    return;
  }
 
  /***************************************************************************
  **
  ** Group rows by investigator
  **
  */
  
  SortedMap<String, Map<MinMax, TimeSpan>> groupByInvestigator() {
    TreeMap<String, Map<MinMax, TimeSpan>> byInvest = new TreeMap<String, Map<MinMax, TimeSpan>>();
    Iterator<TimeSpan> times = getTimeSpans();
    while (times.hasNext()) {
      TimeSpan time = times.next();
      Iterator<Batch> bit = time.getBatches();
      while (bit.hasNext()) {
        Batch bat = bit.next();
        String invest = bat.getInvestigators();
        if ((invest == null) || invest.trim().equals("")) {
          invest = "WJRL_HACKASTIC_KLUDGE";
        }    
        Map<MinMax, TimeSpan> tsMapByInv = byInvest.get(invest);
        if (tsMapByInv == null) {
          tsMapByInv = new HashMap<MinMax, TimeSpan>();
          byInvest.put(invest, tsMapByInv);         
        }
        MinMax tmm = time.getMinMaxSpan();
        TimeSpan tsByInv = tsMapByInv.get(tmm);
        if (tsByInv == null) {
          tsByInv = new TimeSpan(tmm);
          if (time.haveRegionRestrictions()) {
            Iterator<String> grrit = time.getRegionRestrictions();
            while (grrit.hasNext()) {
              tsByInv.addRegionRestriction(grrit.next());
            }
          } 
          tsMapByInv.put(tmm, tsByInv);
        }
        tsByInv.addBatch(bat.clone());
      }
    }
    return (byInvest);
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

    retval.add("perturbation"); 
    retval.add("sources");    
    retval.add("investigators");    
    retval.add("times");    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return the name keyword
  **
  */
  
   static String nameKeyword() {
    return ("name");
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
   public String toString() {
    return ("Perturbation: sources = " + sources_ + " inv = " + investigators_ +
            " timeSpans = " + timeSpans_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
   
  @SuppressWarnings("unused")
  static Perturbation buildFromXML(String elemName, 
                                          Attributes attrs) throws IOException {
    if (!elemName.equals("perturbation")) {
      return (null);
    }

    return (new Perturbation());
  }
  
  /***************************************************************************
  **
  ** Format investigator list into a single string
  **
  */
  
   static String formatInvestigators(List<String> investList) {
    StringBuffer buf = new StringBuffer();
    Iterator<String> iit = investList.iterator();
    int icount = investList.size();
    while (iit.hasNext()) {
      String inv = iit.next();
      buf.append(inv);
      icount--;
      if (icount == 1) {
        buf.append(" & ");
      } else if (icount > 0) {
        buf.append(", ");
      }
    }
    return (buf.toString());
  }
  
  /***************************************************************************
  **
  ** Unformat investigator String into a list (yuk)
  **
  */
  
   static List<String> unformatInvestigators(String invest) {
    
    ArrayList<String> retval = new ArrayList<String>();
    invest = invest.replaceAll("&", ",");
    String[] result = invest.split(",");
    int numRes = result.length;
    for (int i = 0; i < numRes; i++) {
      retval.add(result[i].trim());
    }
    return (retval);
  }
}
