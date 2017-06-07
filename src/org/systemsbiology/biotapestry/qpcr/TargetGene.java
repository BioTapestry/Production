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

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.SortedMap;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.Splitter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

/****************************************************************************
**
** This holds QPCR data for a target gene
*/

class TargetGene {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String notes_;
  private String name_;
  private ArrayList<Perturbation> perturbations_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

   TargetGene(String name, String notes) {
    name_ = name;
    notes_ = notes;
    perturbations_ = new ArrayList<Perturbation>();
  }
  
  /***************************************************************************
  **
  ** Copy Constructor
  */

   TargetGene(TargetGene other) {
    this.notes_ = other.notes_;
    this.name_ = other.name_;

    this.perturbations_ = new ArrayList<Perturbation>();
    Iterator<Perturbation> opit = other.perturbations_.iterator();
    while (opit.hasNext()) {
      this.perturbations_.add(new Perturbation(opit.next()));
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
  
   String getName() {
    return (name_);
  }
  
  /***************************************************************************
  **
  ** Set the name
  */
  
   void setName(String name) {
    name_ = name;
    return;
  }
      
  /***************************************************************************
  **
  ** Get the notes
  **
  */
  
   String getNotes() {
    return (notes_);
  }

  /***************************************************************************
  **
  ** Add a perturbation
  */
  
   void addPerturbation(Perturbation perturbation) {
    perturbations_.add(perturbation);
    return;
  }
  
  /***************************************************************************
  **
  ** Delete the source
  */
  
   void deletePerturbation(int index) {
    perturbations_.remove(index);
    return;
  }
  
  /***************************************************************************
  **
  ** Get an iterator over the perturbations
  */
  
   Iterator<Perturbation> getPerturbations() {
    return (perturbations_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get a perturbation
  */
  
   Perturbation getPerturbation(int i) {
    return (perturbations_.get(i));
  }
  
  /***************************************************************************
  **
  ** Get the set of footnote numbers used by this target entry
  */
  
   Set<String> getFootnoteNumbers() {
    HashSet<String> retval = new HashSet<String>();
    //
    // Footnotes are found here, in a perturbation source, and in measurements
    //    
    Iterator<Perturbation> pers = getPerturbations();
    while (pers.hasNext()) {
      Perturbation per = pers.next();
      Set<String> notes = per.getFootnoteNumbers();
      retval.addAll(notes);
    }
    if (notes_ != null) {
      List<String> foots = Splitter.stringBreak(notes_, ",", 0, true);
      retval.addAll(foots);
    }
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Get the list footnote numbers associated just with this target (not digging down)
  */
  
   List<String> getTranslatedNotes() {
    if (notes_ != null) {
      return (Splitter.stringBreak(notes_, ",", 0, true));
    } else {
      return (new ArrayList<String>());
    }
  }  

  /***************************************************************************
  **
  ** Write the Target Gene to HTML
  **
  */
  
   int writeHTML(PrintWriter out, Indenter ind, ArrayList<MinMax> timeCols, 
                 QpcrTablePublisher qtp, int rowCount, boolean breakOutInvest, 
                 List<String> srcNames, Set<String> usedFootnotes, DataAccessContext dacx) {
    ind.indent();    
    out.println("<tbody>");
    ind.up();
    //
    // Build the gene name string with notes:
    //
    String geneName = name_;
    if (notes_ != null) {
      geneName = geneName + "<sup>" + notes_ + "</sup>";
      usedFootnotes.addAll(getTranslatedNotes());
    }
    
    ArrayList<SortedMap<String, Map<MinMax, TimeSpan>>> perInvest = null;
    int totalCount = 0;
    Iterator<Perturbation> tcpit = getPerturbations();
    while (tcpit.hasNext()) {
      Perturbation p = tcpit.next();
      if (p.matchesForHTML(srcNames)) {
        totalCount++;
      }
    }
 
    if (breakOutInvest) {
      perInvest = new ArrayList<SortedMap<String, Map<MinMax, TimeSpan>>>();
      Iterator<Perturbation> pit = getPerturbations();
      while (pit.hasNext()) {
        Perturbation p = pit.next();
        p.prepForHTML(perInvest, srcNames);
      }
      totalCount = 0;
      int numPI = perInvest.size();
      for (int i = 0; i < numPI; i++) {
        SortedMap<String, Map<MinMax, TimeSpan>> gpi = perInvest.get(i);
        totalCount += gpi.keySet().size();    
      }  
    }
    // Print out the rows:
    Iterator<Perturbation> pit = getPerturbations();
    int count = 0;
    while (pit.hasNext()) {
      Perturbation p = pit.next();
      SortedMap<String, Map<MinMax, TimeSpan>> gpi = (perInvest != null) ? perInvest.get(count) : null; 
      if (p.writeHTML(out, ind, geneName, totalCount, timeCols, qtp, gpi, breakOutInvest, srcNames, dacx)) {
        Set<String> notes = p.getFootnoteNumbers();
        usedFootnotes.addAll(notes);      
        count++;
        geneName = null;
      }
    }      
    ind.down().indent();
    out.println("</tbody>");          
    return (rowCount + totalCount);
  }

  /***************************************************************************
  **
  ** Get the set of single-source perturbation names
  */
  
   Set<String> getSources() {
    HashSet<String> retval = new HashSet<String>();
    Iterator<Perturbation> pit = getPerturbations();
    while (pit.hasNext()) {
      Perturbation p = pit.next();
      if (p.getSourceCount() == 1) {
        String srcName = p.getCombinedSourceName(false);
        retval.add(srcName.toUpperCase().replaceAll(" ", ""));
      }
    }
    return (retval);
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
    retval.add("targetGene");
    retval.add("perturbations");    
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
   public String toString() {
    return ("TargetGene: name = " + name_ + " notes = " + notes_ +
            " perturbations = " + perturbations_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
   static TargetGene buildFromXML(String elemName, 
                                        Attributes attrs) throws IOException {
    if (!elemName.equals("targetGene")) {
      return (null);
    }
    
    String name = null;
    String notes = null;
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
        } else if (key.equals("notes")) {
          notes = val;
        } 
      }
    }
    
    if (name == null) {
      throw new IOException();
    }
    
    return (new TargetGene(name, notes));
  }
}
