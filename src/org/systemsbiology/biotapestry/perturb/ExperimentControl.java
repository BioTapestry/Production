/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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
** This is the description of an experimental control
**
*/

public class ExperimentControl implements Cloneable {

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
  private String description_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Constructor
  */

  public ExperimentControl(String id, String name) {
    id_ = id;
    description_ = name;
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
    return (description_);
  }
    
  /***************************************************************************
  **
  ** Set the description
  */

  public void setDescription(String description) {
    description_ = description;
    return;
  }
 
  /***************************************************************************
  **
  ** Get the display string
  */

  public String getDisplayString() {
    return (description_);
  }

  /***************************************************************************
  **
  ** Clone
  */

  public Object clone() {
    try {
      ExperimentControl newVal = (ExperimentControl)super.clone();
      return (newVal);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();     
    }  
  }
   
  /***************************************************************************
  **
  ** Write the properties to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<experimentControl id=\"");
    out.print(id_);
    out.print("\" desc=\"");
    out.print(CharacterEntityMapper.mapEntities(description_, false));
    out.println("\"/>");
    return;
  }

  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("ExperimentControl: " + id_ + " " + description_ );
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
      
  public static class ExperimentControlWorker extends AbstractFactoryClient {
    
    public ExperimentControlWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("experimentControl");
    }
    
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("experimentControl")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.expControl = buildFromXML(elemName, attrs);
        retval = board.expControl;
      }
      return (retval);     
    }  
        
    private ExperimentControl buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "experimentControl", "id", true);
      String descStr = AttributeExtractor.extractAttribute(elemName, attrs, "experimentControl", "desc", true);      
      descStr = CharacterEntityMapper.unmapEntities(descStr, false);
      return (new ExperimentControl(id, descStr));
    } 
  }
}
