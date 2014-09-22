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

import org.systemsbiology.biotapestry.app.BTState;

/****************************************************************************
**
** This represents a genome item in BioTapestry
*/

public abstract class DBGenomeItem implements GenomeItem, Cloneable {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  protected String name_;
  protected String id_;
  protected BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Copy Constructor
  */

  public DBGenomeItem(DBGenomeItem other) {
    this.appState_ = other.appState_;
    this.name_ = other.name_;
    this.id_ = other.id_;
  }  

  /***************************************************************************
  **
  ** Name and id
  */

  public DBGenomeItem(BTState appState, String name, String id) {
    appState_ = appState;
    name_ = name;
    id_ = id;
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
  public DBGenomeItem clone() {
    try {
      DBGenomeItem retval = (DBGenomeItem)super.clone();
      return (retval);
    } catch (CloneNotSupportedException cnse) {
      throw new IllegalStateException();
    }
  }
  
  /***************************************************************************
  **
  ** Set the name
  ** 
  */
  
  public void setName(String name) {
    name_ = name;
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the name
  ** 
  */
  
  public String getName() {
    return (name_);
  }  
  
  /***************************************************************************
  **
  ** Get the ID
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
    return ("DBGenomeItem: name = " + name_ + " id = " + id_);
  }
  
  /***************************************************************************
  **
  ** Write the item to XML
  **
  */
  
  public void writeXML(PrintWriter out, int indent) {
    out.print("<item ");
    out.print("name=\"");
    out.print(name_);
    out.print("\" id=\"");
    out.print(id_);
    out.println("\" >");
    return;
  }
}
