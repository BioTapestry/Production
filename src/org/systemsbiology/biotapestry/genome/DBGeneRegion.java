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

package org.systemsbiology.biotapestry.genome;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

/****************************************************************************
**
** This represents a region of a gene
*/

public class DBGeneRegion implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  //////////////////////////////////////////////////////////////////////////// 

  private String name_;
  private int startPad_;
  private int endPad_;
  private int evidenceLevel_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** For UI-based construction
  */

  public DBGeneRegion(String name, int startPad, int endPad, int evidenceLevel) {
    name_ = name;
    startPad_ = startPad;
    endPad_ = endPad;
    evidenceLevel_ = evidenceLevel;
  }
  
  /***************************************************************************
  **
  ** For XML-based construction
  */

  public DBGeneRegion(String name, String startPad, String endPad, String evidence) throws IOException {
    name_ = name;
    startPad_ = 0;
    if (startPad != null) {
      try {
        startPad_ = Integer.parseInt(startPad);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    endPad_ = 0;
    if (endPad != null) {
      try {
        endPad_ = Integer.parseInt(endPad);    
      } catch (NumberFormatException nfe) {
        throw new IOException();
      }
    }
    evidenceLevel_ = Gene.LEVEL_NONE;
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
  ** Copy constructor
  */

  public DBGeneRegion(DBGeneRegion other) {
    this.name_ = other.name_;
    this.startPad_ = other.startPad_;
    this.endPad_ = other.endPad_;
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

  public DBGeneRegion clone() {
    try {
      DBGeneRegion retval = (DBGeneRegion)super.clone();        
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Answers if equal:
  */
  
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (!(other instanceof DBGeneRegion)) {
      return (false);
    }
    DBGeneRegion otherDbgr = (DBGeneRegion)other;
    
    if (otherDbgr.startPad_ != this.startPad_) {
      return (false);
    }
    if (otherDbgr.endPad_ != this.endPad_) {
      return (false);
    }
    if (otherDbgr.evidenceLevel_ != this.evidenceLevel_) {
      return (false);
    }    
    if (otherDbgr.name_ == null) {
      return (this.name_ == null);
    }    
    if (!otherDbgr.name_.equals(this.name_)) {
      return (false);
    }
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Get the Name:
  */
  
  public String getName() {
    return (name_);
  }  
  
  /***************************************************************************
  **
  ** Get the start pad
  */
  
  public int getStartPad() {
    return (startPad_);
  }
  
  /***************************************************************************
  **
  ** Get the end pad
  */
  
  public int getEndPad() {
    return (endPad_);
  }
  
  /***************************************************************************
  **
  ** Get the evidence level
  */
  
  public int getEvidenceLevel() {
    return (evidenceLevel_);
  }  
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("DBGeneRegion: name = " + name_ + " startPad = " + startPad_ 
            + " endPad = " + endPad_ + " evidence = " + evidenceLevel_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<region");
    out.print(" name=\"");
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    out.print("\" startPad=\"");
    out.print(startPad_);
    out.print("\" endPad=\"");
    out.print(endPad_);
    if (evidenceLevel_ != Gene.LEVEL_NONE) {
      out.print("\" evidence=\"");
      out.print(evidenceLevel_);    
    }   
    out.println("\" />");        
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  

  /***************************************************************************
  **
  ** Handle region creation
  **
  */
  
  public static DBGeneRegion buildFromXML(Genome genome,
                                          Attributes attrs) throws IOException {
    String name = null;
    String startPad = null;
    String endPad = null;
    String evidence = null;
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
        } else if (key.equals("startPad")) {
          startPad = val;
        } else if (key.equals("endPad")) {
          endPad = val;
        } else if (key.equals("evidence")) {
          evidence = val;
        }
      }
    }
    return (new DBGeneRegion(name, startPad, endPad, evidence));
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("region");
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor.  Do not use
  */

  protected DBGeneRegion() {
  }    
}
