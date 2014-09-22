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

package org.systemsbiology.biotapestry.ui;

import java.io.IOException;
import java.io.PrintWriter;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;


/****************************************************************************
**
** A class holding info on layout creation history
*/

public class LayoutMetadata implements Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
   
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
     
  private LayoutDerivation ld_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Construction
  */   
  
  public LayoutMetadata() {
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

  public LayoutMetadata clone() {
    try {
      LayoutMetadata retval = (LayoutMetadata)super.clone();
      if (this.ld_ != null) {
        retval.ld_ = (LayoutDerivation)this.ld_.clone();
      }
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  

  /***************************************************************************
  **
  ** Get the layout derivation
  **
  */
  
  public LayoutDerivation getDerivation() {
    return (ld_);
  }
  
  /***************************************************************************
  **
  ** Set the layout derivation
  **
  */
  
  public void setDerivation(LayoutDerivation ld) {
    ld_ = ld;
    return;
  }  
   
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.print("<layoutMetadata");
    if ((ld_ == null) || (ld_.numDirectives() == 0)) {
      out.println(" />");
    } else {
      out.println(">");
      ind.up();
      ld_.writeXML(out, ind);
      ind.down().indent();
      out.println("</layoutMetadata>");
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Return the element keyword that we are interested in
  **
  */
  
  public static String keywordOfInterest() {
    return ("layoutMetadata");
  }   

  /***************************************************************************
  **
  ** Handle creation from XML
  **
  */
  
  public static LayoutMetadata buildFromXML(Attributes attrs) throws IOException {    
    LayoutMetadata lm = new LayoutMetadata();
    return (lm);
  }  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
