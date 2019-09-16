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

import java.io.IOException;
import java.io.PrintWriter;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;


/****************************************************************************
**
** This describes the conditions of a perturbation experiment, such
** as "PAM3" or "LPS".  Not very fleshed out at the moment
**
*/

public class ExperimentConditions implements Cloneable {

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

  private String id_;
  private String conditionDescription_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public ExperimentConditions(String id, String name) {
    id_ = id;
    conditionDescription_ = name;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the id
  */

  public String getID() {
    return (id_);
  }
  
  /***************************************************************************
  **
  ** Get the description
  */

  public String getDescription() {
    return (conditionDescription_);
  }
    
  /***************************************************************************
  **
  ** Set the description
  */

  public void setDescription(String conditionDescription) {
    conditionDescription_ = conditionDescription;
    return;
  }
 
  /***************************************************************************
  **
  ** Get the display string
  */

  public String getDisplayString() {
    return (conditionDescription_);
  }

  /***************************************************************************
  **
  ** Clone
  */

  @Override
  public ExperimentConditions clone() {
    try {
      ExperimentConditions newVal = (ExperimentConditions)super.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }  
  }
   
  /***************************************************************************
  **
  ** Standard equals
  **
  */  

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof ExperimentConditions)) {
      return (false);
    }
    ExperimentConditions otherEC = (ExperimentConditions)other;
    if (!this.id_.equals(otherEC.id_)) {
      return (false);
    }
    return (this.conditionDescription_.equals(otherEC.conditionDescription_));
  }  
  
 
  /***************************************************************************
  **
  ** Write the properties to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<experimentCond id=\"");
    out.print(id_);
    out.print("\" desc=\"");
    out.print(CharacterEntityMapper.mapEntities(conditionDescription_, false));
    out.println("\"/>");
    return;
  }

  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("ExperimentConditions: " + id_ + " " + conditionDescription_ );
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For XML I/O
  ** 
  */ 
      
  public static class ExperimentCondWorker extends AbstractFactoryClient {
    
    public ExperimentCondWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("experimentCond");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("experimentCond")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.experCond = buildFromXML(elemName, attrs);
        retval = board.experCond;
      }
      return (retval);     
    }  
        
    private ExperimentConditions buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "experimentCond", "id", true);
      String descStr = AttributeExtractor.extractAttribute(elemName, attrs, "experimentCond", "desc", true);      
      descStr = CharacterEntityMapper.unmapEntities(descStr, false);
      return (new ExperimentConditions(id, descStr));
    } 
  }
}
