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
import java.util.Set;
import java.util.HashSet;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.util.Indenter;

/****************************************************************************
**
** This represents a member of a group of nodes in BioTapestry
*/

public class GroupMember implements Cloneable {
  
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

  public GroupMember(GroupMember other) {
    this.id_ = other.id_;
  }  
  
  /***************************************************************************
  **
  ** Make a member for the given gene
  */

  public GroupMember(String id) {
    id_ = id.trim();
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
  
  public GroupMember clone() { 
    try {
      return ((GroupMember)super.clone());
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
    return ("GroupMember: id = " + id_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();     
    out.print("<member ");
    out.print("ref=\"");
    out.print(id_);
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
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("member");    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Handle creation from XML
  **
  */
  
  public static GroupMember buildFromXML(Attributes attrs) throws IOException {
                                             
    String id = null;
        
    if (attrs != null) {
      int count = attrs.getLength();
      for (int i = 0; i < count; i++) {
        String key = attrs.getQName(i);
        if (key == null) {
          continue;
        }
        String val = attrs.getValue(i);
        if (key.equals("ref")) {
          id = val;
        } 
      }
    }
    
    if (id == null) {
      throw new IOException();
    }
   
    return (new GroupMember(id));
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
  private GroupMember() {
  }  
}
