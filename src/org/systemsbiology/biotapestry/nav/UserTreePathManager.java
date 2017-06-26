/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

import java.util.Set;
import java.util.HashSet;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.systemsbiology.biotapestry.util.Indenter;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.TaggedSet;
import org.systemsbiology.biotapestry.util.UniqueLabeller;

/****************************************************************************
**
** UserTreePathManager.
*/

public class UserTreePathManager {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashMap<String, UserTreePath> paths_;
  private ArrayList<String> pathOrder_;
  private UniqueLabeller labels_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */
    
  public UserTreePathManager() {
    paths_ = new HashMap<String, UserTreePath>();
    pathOrder_ = new ArrayList<String>();
    labels_ = new UniqueLabeller();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Clear out the path library
  */
    
  public void dropAllPaths() {
    paths_.clear();
    pathOrder_.clear();
    labels_ = new UniqueLabeller();
    return;
  }  

  /***************************************************************************
  ** 
  ** Get the count of paths
  */

  public int getPathCount() {
    return (paths_.size());
  }  
  
  /***************************************************************************
  ** 
  ** Get the tree path associated with the given key
  */

  public UserTreePath getPath(String key) {
    return (paths_.get(key));
  }
  
  /***************************************************************************
  ** 
  ** Get the index tree path associated with the given key
  */

  public int getPathIndex(String key) {
    return (pathOrder_.indexOf(key));
  }  
  
  /***************************************************************************
  ** 
  ** Get the key for the given index
  */

  public String getPathKey(int index) {
    String key = pathOrder_.get(index);
    return (key);
  }  
    
  /***************************************************************************
  ** 
  ** Add a new tree path at given index
  */

  public UserTreePathChange addPath(String pathName, int index) {
    if (index > pathOrder_.size()) {
      throw new IllegalArgumentException();
    }
    UserTreePathChange utpc = new UserTreePathChange();
    UserTreePath newPath = new UserTreePath(pathName);
    String nextLabel = labels_.getNextLabel();
    newPath.setID(nextLabel);
    paths_.put(nextLabel, newPath);
    pathOrder_.add(index, nextLabel);
    utpc.newIndex = index;
    utpc.newPath = newPath.clone();
    return (utpc);
  }  
  
  /***************************************************************************
  ** 
  ** Add a new tree path stop at given index
  */

  public UserTreePathChange addPathStop(String pathKey, int stopIndex,UserTreePathStop newStop) {
    UserTreePathChange utpc = new UserTreePathChange();
    utpc.oldIndex = -1;
    UserTreePath currPath = paths_.get(pathKey);
    utpc.oldPath = currPath.clone();
    
    currPath.addStop(newStop, stopIndex);       
    utpc.newIndex = -1;
    utpc.newPath = currPath.clone();
    return (utpc);
  }  

  /***************************************************************************
  ** 
  ** Add a preexisting tree path (I/O)
  */

  public void addPreexistingPath(UserTreePath newPath) {
    labels_.addExistingLabel(newPath.getID());
    paths_.put(newPath.getID(), newPath);
    pathOrder_.add(newPath.getID());
    return;
  }    
  
  /***************************************************************************
  ** 
  ** Delete a path
  */

  public UserTreePathChange deletePath(String key) {
    UserTreePathChange utpc = new UserTreePathChange();
    utpc.oldIndex = pathOrder_.indexOf(key);
    UserTreePath delPath = paths_.get(key);
    utpc.oldPath = delPath.clone();
    paths_.remove(key);
    labels_.removeLabel(key);
    pathOrder_.remove(key);
    return (utpc);
  }
   
  /***************************************************************************
  **
  ** Write the path library to XML
  **
  */
  
  public void writeXML(PrintWriter out, Indenter ind) {
    ind.indent();
    out.println("<userTreePaths>");
    ind.up();
    Iterator<String> skit = pathOrder_.iterator();
    while (skit.hasNext()) {
      String key = skit.next();
      UserTreePath utPath = paths_.get(key);
      utPath.writeXML(out, ind);
    }
    ind.down().indent();
    out.println("</userTreePaths>");
    return;
  }
  
  /***************************************************************************
  ** 
  ** Delete the path stop at given index
  */

  public UserTreePathChange deletePathStop(String pathKey, int stopIndex) {
    UserTreePathChange utpc = new UserTreePathChange();
    utpc.oldIndex = -1;
    UserTreePath currPath = paths_.get(pathKey);
    utpc.oldPath = currPath.clone();
    currPath.deleteStop(stopIndex);
    utpc.newIndex = -1;
    utpc.newPath = currPath.clone();
    return (utpc);
  }
  
  /***************************************************************************
  ** 
  ** Replace the path stop at given index
  */

  public UserTreePathChange replacePathStop(String pathKey, int stopIndex, String newKey) {
    UserTreePathChange utpc = new UserTreePathChange();
    utpc.oldIndex = -1;
    UserTreePath currPath = paths_.get(pathKey);
    utpc.oldPath = currPath.clone();
    currPath.replaceStopModelID(stopIndex, newKey);
    utpc.newIndex = -1;
    utpc.newPath = currPath.clone();
    return (utpc);
  }  
  
  /***************************************************************************
  ** 
  ** Drop the module from the path stop at the given index
  */

  public UserTreePathChange dropPathStopModule(String pathKey, int stopIndex, String modKey) {
    UserTreePathChange utpc = new UserTreePathChange();
    utpc.oldIndex = -1;
    UserTreePath currPath = paths_.get(pathKey);
    utpc.oldPath = currPath.clone();
    currPath.dropStopModule(stopIndex, modKey);
    utpc.newIndex = -1;
    utpc.newPath = currPath.clone();
    return (utpc);
  }  
  
  /***************************************************************************
  ** 
  ** Delete all path stops for the given model
  */

  public UserTreePathChange[] deleteAllPathStopsForModel(String modelKey) {
    ArrayList<UserTreePathChange> retvals = new ArrayList<UserTreePathChange>();
    ArrayList<String> deadPaths = new ArrayList<String>();
    Iterator<String> pit = paths_.keySet().iterator();
    while (pit.hasNext()) {
      String pathKey = pit.next();
      UserTreePath currPath = paths_.get(pathKey);
      int stopNum = currPath.getStopCount();
      int stopIndex = 0;
      while (stopIndex < stopNum) {
        UserTreePathStop currStop = currPath.getStop(stopIndex);
        if (currStop.getGenomeID().equals(modelKey)) {
          retvals.add(deletePathStop(pathKey, stopIndex));
          stopNum--;
          continue;
        }
        stopIndex++;
      }
      if (stopNum == 0) {
        deadPaths.add(pathKey);
      }
    }
    
    //
    // Kill off empty paths:
    //
    
    Iterator<String> dpit = deadPaths.iterator();
    while (dpit.hasNext()) {
      String deadKey = dpit.next();
      retvals.add(deletePath(deadKey));
    }
    
    UserTreePathChange[] retval = new UserTreePathChange[retvals.size()];
    retvals.toArray(retval);    
    return (retval);
  }  

  /***************************************************************************
  **
  ** Undo a tree path change
  */
  
  public boolean changeUndo(UserTreePathChange undo) {
    boolean retval = false;
    if (undo.oldPath != null) {
      String oldKey = undo.oldPath.getID();
      if (undo.oldIndex != -1) {  // handling a deletion: restore it
        labels_.addExistingLabel(oldKey);
        paths_.put(oldKey, undo.oldPath.clone());
        pathOrder_.add(undo.oldIndex, oldKey);        
      } else { // handling a replacement
        paths_.put(oldKey, undo.oldPath.clone());               
      }
    } else if (undo.newPath != null) {  // handling an addition: delete it
      String newKey = undo.newPath.getID();
      if (undo.newIndex != -1) {
        paths_.remove(newKey);
        labels_.removeLabel(newKey);
        pathOrder_.remove(newKey);
        retval = true;
      } else { // handling a replacement
        throw new IllegalArgumentException();    
      }      
    } else {
      throw new IllegalArgumentException();
    }
    return (retval);
  }
    
  /***************************************************************************
  **
  ** Redo a tree path change
  */
  
  public boolean changeRedo(UserTreePathChange redo) {
    boolean retval = false;
    if (redo.newPath != null) {
      String newKey = redo.newPath.getID();
      if (redo.newIndex != -1) {  // handling an addition, restore it
        labels_.addExistingLabel(newKey);
        paths_.put(newKey, redo.newPath.clone());
        pathOrder_.add(redo.newIndex, newKey);        
      } else { // handling a replacement
        paths_.put(newKey, redo.newPath.clone());               
      }
    } else if (redo.oldPath != null) {  // handling a deletion: delete it
      String oldKey = redo.oldPath.getID();
      if (redo.oldIndex != -1) {
        paths_.remove(oldKey);
        labels_.removeLabel(oldKey);
        pathOrder_.remove(oldKey);
        retval = true;
      } else { // handling a replacement
        throw new IllegalArgumentException();    
      }      
    } else {
      throw new IllegalArgumentException();
    }  
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return possible path choices
  */
  
  public Vector<ObjChoiceContent> getPathChoices() {
    Vector<ObjChoiceContent> retval = new Vector<ObjChoiceContent>();
    Iterator<String> skit = pathOrder_.iterator();
    while (skit.hasNext()) {
      String key = skit.next();
      UserTreePath utPath = paths_.get(key);
      retval.add(new ObjChoiceContent(utPath.getName(), key));
    }    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Return a path choice for the given key
  */
  
  public ObjChoiceContent getPathChoice(String key) {
    UserTreePath utPath = paths_.get(key);
    return (new ObjChoiceContent(utPath.getName(), key));
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public static Set<String> keywordsOfInterest() {
    HashSet<String> retval = new HashSet<String>();
    retval.add("userTreePaths");
    return (retval);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
}
