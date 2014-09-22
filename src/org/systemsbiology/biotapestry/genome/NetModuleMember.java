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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.util.AttributeExtractor;

/****************************************************************************
**
** This represents a member of a module
*/

public class NetModuleMember implements Cloneable {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String id_;
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy constructor
  */

  public NetModuleMember(NetModuleMember other) {
    this.id_ = other.id_;
  }  
  
  /***************************************************************************
  **
  ** Copy constructor with remapping of ID
  */

  public NetModuleMember(NetModuleMember other, Map<String, String> nodeMap) {
    if ((other.id_ != null) && (nodeMap != null)) {
      this.id_ = nodeMap.get(other.id_);
      if (this.id_ == null) {
        throw new IllegalStateException();
      }
    } else {
      this.id_ = other.id_;
    }
  }   
  
  /***************************************************************************
  **
  ** Make a member for the given node ID
  */

  public NetModuleMember(String id) {
    id_ = id;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Clone support
  ** 
  */  
  
  public NetModuleMember clone() { 
    try {
      return ((NetModuleMember)super.clone());
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }  

  /***************************************************************************
  **
  ** Get the id
  ** 
  */
  
  public String getID() {
    return (id_);
  }  
  
  /***************************************************************************
  **
  ** Standard toString():
  ** 
  */
  
  public String toString() {
    return ("NetModuleMember: id = " + id_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent(); 
    out.print("<modMember ref=\"");
    out.print(id_);
    out.println("\" />");
    return;
  }
  
  /***************************************************************************
  **
  ** Standard equals:
  **
  */
  
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (other == null) {
      return (false);
    }
    if (!(other instanceof NetModuleMember)) {
      return (false);
    }
    NetModuleMember otherNmm = (NetModuleMember)other;   
    return (this.id_.equals(otherNmm.id_));
  }
  
  /***************************************************************************
  **
  ** Hashcode:
  **
  */
  
  public int hashCode() {
    return (id_.hashCode());
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class NetModuleMemberWorker extends AbstractFactoryClient {
    
    public NetModuleMemberWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("modMember");
    }

    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("modMember")) {
        retval = buildFromXML(elemName, attrs);
      }
      return (retval);
    }

    private NetModuleMember buildFromXML(String elemName, Attributes attrs) throws IOException {
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "modMember", "ref", true);
      return (new NetModuleMember(id));
    } 
  }    

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor  Do not use
  */

  @SuppressWarnings("unused")
  private NetModuleMember() {
  }  
}
