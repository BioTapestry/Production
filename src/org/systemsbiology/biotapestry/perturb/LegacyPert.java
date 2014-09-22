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

import org.systemsbiology.biotapestry.util.AttributeExtractor;

/****************************************************************************
**
** Old qpcr data frequently had only "NS" instead of a numerical value.
** Also, for unaffected genes, we didn't have info on the number or values
** of the time points.  Store this info
*/

public class LegacyPert implements Cloneable {
  public String oldValue;
  public boolean unknownMultiCount;
  
  /***************************************************************************
  **
  ** Constructor
  */

  public LegacyPert(String oldValue) {
    this.oldValue = oldValue;
    unknownMultiCount = false; 
  }
 
  /***************************************************************************
  **
  ** Constructor
  */

  public LegacyPert(String oldValue, boolean unknownMultiCount) {
    this.oldValue = oldValue;
    this.unknownMultiCount = unknownMultiCount; 
  }
   
  /***************************************************************************
  **
  ** Clone
  */

  public Object clone() {
    try {
      LegacyPert newVal = (LegacyPert)super.clone();
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
  
  public boolean equals(Object other) {
    if (other == null) {
      return (false);
    }
    if (other == this) {
      return (true);
    }
    if (!(other instanceof LegacyPert)) {
      return (false);
    }
    LegacyPert otherLP = (LegacyPert)other;
    if (!this.oldValue.equals(otherLP.oldValue)) {
      return (false);
    }
    return (this.unknownMultiCount == otherLP.unknownMultiCount);
  }  
 
  /***************************************************************************
  **
  ** Write the measurement to XML
  **
  */
  
  public void writeXMLSupport(PrintWriter out) {
    out.print("legV=\"");
    out.print(oldValue);
    out.print("\"");
    if (unknownMultiCount) {
      out.print(" mulc=\"true\"");
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Read the measurement from XML
  **
  */
  
  public static LegacyPert buildFromXMLSupport(String elemName, Attributes attrs) throws IOException {    
    String legVal = AttributeExtractor.extractAttribute(elemName, attrs, elemName, "legV", false);
    String mulc = AttributeExtractor.extractAttribute(elemName, attrs, elemName, "mulc", false);
    if (legVal == null) {
      return (null);
    }
    if (mulc != null) {
      boolean multiCount = Boolean.valueOf(mulc).booleanValue();
      return (new LegacyPert(legVal, multiCount));
    } else {
      return (new LegacyPert(legVal));
    }
  }
}