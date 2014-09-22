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

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.util.Indenter;

/****************************************************************************
**
** This represents an internal function node
*/

public class InternalFunction extends DBGenomeItem {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private static final String AND_TAG_ = "AND";
  private static final String OR_TAG_ = "OR";
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  protected int functionType_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  public static final int AND_FUNCTION = 0;
  public static final int OR_FUNCTION  = 1;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public InternalFunction(InternalFunction other) {
    super(other);
    this.functionType_ = other.functionType_;
  }
  
  /***************************************************************************
  **
  ** For UI construction
  */

  public InternalFunction(BTState appState, int functionType, String id) {
    super(appState, null, id);
    functionType_ = functionType;
  }  

  /***************************************************************************
  **
  ** For XML based construction
  */

  public InternalFunction(BTState appState, String function, String id) throws IOException {
    super(appState, null, id);
    if (function == null) {
      throw new IOException();
    }
    
    if (function.equals(AND_TAG_)) {
      functionType_ = AND_FUNCTION;
    } else if (function.equals(OR_TAG_)) {
      functionType_ = OR_FUNCTION;
    } else {
      throw new IOException();
    }
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

  public InternalFunction clone() {
    InternalFunction retval = (InternalFunction)super.clone();
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Get function type
  */

  public int getFunctionType() {
    return (functionType_);
  }
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
     return ("InternalFunction: id = " + id_ + " type = " + getTag());
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<internalFunction id=\"");
    out.print(id_);
    out.print("\" type=\"");
    out.print(getTag());
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
  ** Handle Group creation
  **
  */
  
  public static InternalFunction buildFromXML(BTState appState, Genome genome,
                                              Attributes attrs) throws IOException {

    String id = null;
    String func = null;
    
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("id")) {
          id = val;
        } else if (key.equals("type")) {
          func = val;
        } 
      }
    }
    return (new InternalFunction(appState, func, id));
  }  

  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("internalFunction");
    return (retval);
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get XML tag
  */

  private String getTag() {
    switch (functionType_) {
      case AND_FUNCTION:
        return (AND_TAG_);
      case OR_FUNCTION:
        return (OR_TAG_);
      default:
        throw new IllegalStateException();
    }
  }
}
