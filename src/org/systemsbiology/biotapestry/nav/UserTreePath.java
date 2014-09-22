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

package org.systemsbiology.biotapestry.nav;

import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;
import org.systemsbiology.biotapestry.util.AttributeExtractor;
import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.CharacterEntityMapper;

/****************************************************************************
**
** This holds a user-defined path through the navigation tree
*/

public class UserTreePath implements Cloneable {
  
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
  private String name_;  
  private ArrayList<UserTreePathStop> stops_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public UserTreePath(UserTreePath other) {
    this.id_ = other.id_;
    this.name_ = other.name_;
    this.stops_ = new ArrayList<UserTreePathStop>();
    int size = other.stops_.size();
    for (int i = 0; i < size; i++) {
      UserTreePathStop utpStop = other.stops_.get(i);
      this.stops_.add(utpStop.clone());
    }
  }   

  /***************************************************************************
  **
  ** Constructor
  */

  public UserTreePath(String id, String name) {
    id_ = id;
    name_ = name;    
    stops_ = new ArrayList<UserTreePathStop>();
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  public UserTreePath(String name) {
    id_ = null;
    name_ = name;    
    stops_ = new ArrayList<UserTreePathStop>();
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

  public UserTreePath clone() {
    try {
      UserTreePath retval = (UserTreePath)super.clone();
      retval.stops_ = new ArrayList<UserTreePathStop>();
      int size = this.stops_.size();
      for (int i = 0; i < size; i++) {
        UserTreePathStop utpStop = this.stops_.get(i);
        retval.stops_.add(utpStop.clone());
      }
      return (retval);
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException();
    }
  }  
 
  /***************************************************************************
  **
  ** Get the name
  */
  
  public String getName() {
    return (name_);
  }
  
  /***************************************************************************
  **
  ** Set the name
  */
  
  public void setName(String name) {
    name_ = name;
    return;
  }
  
  /***************************************************************************
  **
  ** Get the ID
  */
  
  public String getID() {
    return (id_);
  }
  
  /***************************************************************************
  **
  ** Set the ID
  */
  
  public void setID(String id) {
    id_ = id;
    return;
  }
  
  /***************************************************************************
  **
  ** Add a path stop at the given index
  */
  
  public void addStop(UserTreePathStop pathStop, int stopIndex) {
    if (stopIndex > stops_.size()) {
      throw new IllegalArgumentException();
    }
    stops_.add(stopIndex, pathStop);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a path stop at the given end (IO usage)
  */
  
  public void addStop(UserTreePathStop pathStop) {
    stops_.add(pathStop);
    return;
  }  

  /***************************************************************************
  **
  ** Delete a path stop
  */
  
  public void deleteStop(int i) {
    stops_.remove(i);
    return;
  }  
  
  /***************************************************************************
  **
  ** Replace the model for a path stop
  */
  
  public void replaceStopModelID(int i, String newID) {
    UserTreePathStop stop = stops_.get(i);
    stop.setGenomeID(newID);
    return;
  } 
  
  /***************************************************************************
  **
  ** Drop a module from a path stop
  */
  
  public void dropStopModule(int i, String moduleID) {
    UserTreePathStop stop = stops_.get(i);
    stop.dropModKey(moduleID);
    return;
  }   
 
  /***************************************************************************
  **
  ** Get the stop size
  */
  
  public int getStopCount() {
    return (stops_.size());
  }    
  
  /***************************************************************************
  **
  ** Get the nth stop
  */
  
  public UserTreePathStop getStop(int n) {
    return (stops_.get(n));
  }  
  
  /***************************************************************************
  **
  ** Answer if we have the stop
  */
  
  public boolean containsStop(UserTreePathStop stop) {
    return (stops_.contains(stop));
  }   
  
  /***************************************************************************
  **
  ** Write the User Tree Path to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();    
    out.print("<userTreePath id=\"");
    out.print(id_);
    out.print("\" name=\"");    
    out.print(CharacterEntityMapper.mapEntities(name_, false));
    int numStops = stops_.size();
    if (numStops == 0) {
      out.println("\" />");
      return;
    }    
    out.println("\">");
    ind.up();
    for (int i = 0; i < numStops; i++) {
      UserTreePathStop pathStop = getStop(i);
      pathStop.writeXML(out, ind);
    }
    ind.down().indent();       
    out.println("</userTreePath>");
    return;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** For XML I/O
  */  
      
  public static class UserTreePathWorker extends AbstractFactoryClient {
    
    public UserTreePathWorker(FactoryWhiteboard whiteboard) {
      super(whiteboard);
      myKeys_.add("userTreePath");
      installWorker(new UserTreePathStop.UserTreePathStopWorker(whiteboard), new MyGlue());
    }
  
    protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {
      Object retval = null;
      if (elemName.equals("userTreePath")) {
        FactoryWhiteboard board = (FactoryWhiteboard)this.sharedWhiteboard_;
        board.userTreePath = buildFromXML(elemName, attrs);
        retval = board.userTreePath;
      }
      return (retval);     
    }

    private UserTreePath buildFromXML(String elemName, Attributes attrs) throws IOException {  
      String id = AttributeExtractor.extractAttribute(elemName, attrs, "userTreePath", "id", true);
      String name = AttributeExtractor.extractAttribute(elemName, attrs, "userTreePath", "name", true);
      name = CharacterEntityMapper.unmapEntities(name, false);
      return (new UserTreePath(id, name));
    }
  }
  
  public static class MyGlue implements GlueStick {
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      UserTreePath path = board.userTreePath;
      UserTreePathStop pathStop = board.userTreePathStop;
      path.addStop(pathStop);
      return (null);
    }
  }  
}
