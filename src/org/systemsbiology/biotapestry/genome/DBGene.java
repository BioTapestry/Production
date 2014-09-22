/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.genome;

import java.io.PrintWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Vector;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** This represents a gene in BioTapestry
*/

public class DBGene extends DBNode implements Gene {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int DEFAULT_PAD_COUNT = 7;
  public static final int MAX_PAD_COUNT = 30;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private ArrayList<DBGeneRegion> regions_;
  private int evidenceLevel_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Name and id
  */

  public DBGene(BTState appState, String name, String id) {
    super(appState, GENE, name, id);
    regions_ = new ArrayList<DBGeneRegion>();
    evidenceLevel_ = LEVEL_NONE;
  }

  /***************************************************************************
  **
  ** Name and id and evidence:
  */

  public DBGene(BTState appState, String name, String id, String evidence, String size) throws IOException {
    super(appState, GENE_TAG, name, id, size);
    regions_ = new ArrayList<DBGeneRegion>();
    evidenceLevel_ = LEVEL_NONE;
    if (evidence != null) {
      try {
        evidenceLevel_ = Integer.parseInt(evidence);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }

  }  
  
  /***************************************************************************
  **
  ** Downcast
  */

  public DBGene(DBNode node) {
    super(node);
    nodeType_ = GENE;
    regions_ = new ArrayList<DBGeneRegion>();
    evidenceLevel_ = LEVEL_NONE;
    if (padCount_ < DEFAULT_PAD_COUNT) {
      padCount_ = DEFAULT_PAD_COUNT;
    }
  }  

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public DBGene(DBGene other) {
    super(other);
    this.regions_ = new ArrayList<DBGeneRegion>();
    Iterator<DBGeneRegion> rit = other.regions_.iterator();
    while (rit.hasNext()) {
      DBGeneRegion reg = rit.next();
      this.regions_.add(new DBGeneRegion(reg));
    }
    this.evidenceLevel_ = other.evidenceLevel_;
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
  public DBGene clone() {
    DBGene retval = (DBGene)super.clone();
    retval.regions_ = new ArrayList<DBGeneRegion>();
    Iterator<DBGeneRegion> rit = this.regions_.iterator();
    while (rit.hasNext()) {
      DBGeneRegion reg = rit.next();
      retval.regions_.add(reg.clone());
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
 
  @Override
  public String toString() {
    return ("DBGene: name = " + name_ + " id = " + id_ + " regions = " + regions_ + 
            " evidence = " + evidenceLevel_);
  }
  
  /***************************************************************************
  **
  ** Get display string.  Override to pay attention to type preface
  */

  @Override
  public String getDisplayString(Genome genome, boolean typePreface) {
    ResourceManager rMan = appState_.getRMan();
    String retval;
    if (typePreface) { 
      String format = rMan.getString("ncreate.importFormat");
      String typeDisplay = mapTypeToDisplay(rMan, nodeType_);
      retval = MessageFormat.format(format, new Object[] {typeDisplay, getName()});
    } else {
      String format = rMan.getString("ncreate.importFormatNoType");
      retval = MessageFormat.format(format, new Object[] {getName()});
    }
    return (retval);
  }     
  
  /***************************************************************************
  **
  ** Add a region
  */
  
  public void addRegion(DBGeneRegion region) {
    regions_.add(region);
    return;
  }
  
  /***************************************************************************
  **
  ** Replace the region list
  */
  
  public void setRegions(List<DBGeneRegion> newRegions) {
    Iterator<DBGeneRegion> nrit = newRegions.iterator();
    regions_.clear();
    while (nrit.hasNext()) {
      DBGeneRegion region = nrit.next();
      regions_.add(new DBGeneRegion(region));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get a region iterator
  */
  
  public Iterator<DBGeneRegion> regionIterator() {
    return (regions_.iterator());
  }
  
  /***************************************************************************
  **
  ** Get a region count
  */
  
  public int getNumRegions() {
    return (regions_.size());
  }
  
  /***************************************************************************
  **
  ** Get the experimental verification level
  */
  
  public int getEvidenceLevel() {
    return (evidenceLevel_);  
  }
  
  /***************************************************************************
  **
  ** Set the experimental verification level
  */
  
  public void setEvidenceLevel(int evidenceLevel) {
    evidenceLevel_ = evidenceLevel;
    return;
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */

  @Override
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<");
    out.print(GENE_TAG);
    out.print(" name=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    out.print("\" id=\"");
    out.print(id_);
    if (evidenceLevel_ != LEVEL_NONE) {
      out.print("\" evidence=\"");
      out.print(evidenceLevel_);    
    } 
    // FIX ME: Should be handled by a super.xmlSupport()-type of call:
    if (padCount_ != DEFAULT_PAD_COUNT) {
      out.print("\" size=\"");
      out.print(padCount_);    
    } 
    
    int numURL = urls_.size();
    if ((regions_.size() == 0) && (logic_ == null) && (description_ == null) && (numURL == 0)) {
      out.println("\" />");
      return;
    } 
    
    out.println("\" >");
    
    if (regions_.size() != 0) {
      ind.up();
      Iterator<DBGeneRegion> regs = regions_.iterator();
      while (regs.hasNext()) {
        DBGeneRegion reg = regs.next();
        reg.writeXML(out, ind);
      }
      ind.down();
    }
      
    writeLogicDescUrlToXML(out, ind);
    
    ind.indent(); 
    out.print("</");
    out.print(GENE_TAG);
    out.println(">");          
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Handle Gene creation
  **
  */
  
  public static DBGene buildFromXML(BTState appState, Genome genome,
                                    Attributes attrs) throws IOException {
    String name = null;
    String id = null;
    String evidence = null;
    String size = null;
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
        } else if (key.equals("id")) {
          id = val;
        } else if (key.equals("evidence")) {
          evidence = val;
        } else if (key.equals("size")) {
          size = val;
        }
      }
    }
    return (new DBGene(appState, name, id, evidence, size));
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(GENE_TAG);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Evidence levels
  **
  */

  public static Set<String> evidenceLevels() {
    HashSet<String> retval = new HashSet<String>();
    retval.add(LEV_NONE_STR);
    retval.add(LEV_1_STR);
    return (retval);
  }  
  
  /***************************************************************************
  **
  ** Return possible evidence choices
  */
  
  public static Vector<ChoiceContent> getEvidenceChoices(BTState appState) {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    for (int i = 0; i < NUM_EVIDENCE_LEVELS; i++) {
      retval.add(evidenceTypeForCombo(appState, i));    
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get a combo box element
  */
  
  public static ChoiceContent evidenceTypeForCombo(BTState appState, int eviLev) {
    return (new ChoiceContent(appState.getRMan().getString("nprop." + mapToEvidenceTag(eviLev)), eviLev));
  }

  /***************************************************************************
  **
  ** Map evidence values
  */

  public static String mapToEvidenceTag(int val) {
    switch (val) {
      case LEVEL_NONE:
        return (LEV_NONE_STR);
      case LEVEL_1:
        return (LEV_1_STR);
      default:
        throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Map evidence strings to values
  */

  public static int mapFromEvidenceTag(String tag) {
    if (tag.equals(LEV_NONE_STR)) {
      return (LEVEL_NONE);
    } else if (tag.equals(LEV_1_STR)) {
      return (LEVEL_1);
    } else {
      throw new IllegalArgumentException();
    }
  }
}
