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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.prefs.Preferences;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.MainCommands;

/****************************************************************************
**
** Tracks the most recent files used.
*/

public class RecentFilesManager {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private static final int HISTORY_DEPTH_ = 6;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<String> absPaths_;
  private HashMap<String, String> names_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */

  public RecentFilesManager(BTState appState) {
    appState_ = appState;
    absPaths_ = new ArrayList<String>();
    names_ = new HashMap<String, String>();
    loadFromPrefs();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Drop the current settings
  */

  public void drop() {
    absPaths_.clear();
    names_.clear();
    commitToPrefs();    
    return;
  }
  
  /***************************************************************************
  ** 
  ** Get an iterator over the recent files:
  */

  public Iterator<String> getRecentFiles() {
    return (absPaths_.iterator());   
  }
  
  /***************************************************************************
  ** 
  ** Set the latest file:
  */

  public String getRecentFileName(String absPath) {
    return (names_.get(absPath));   
  }  
  
  /***************************************************************************
  ** 
  ** Set the latest file:
  */

  public void updateLatestFile(File recentFile) {
    
    //
    // At least on Linux, I am seeing that preferences are not committed
    // until the program exits, and preferences are only loaded at startup.
    // Is this standard across all platforms?  If not, this is an attempt
    // to keep two running executions in rough sync as changes are made.
    // With the Linux model, this should have no effect.
    //
    
    if (canLoadFromPrefs()) { 
      loadFromPrefs();
    }

    String absPath = recentFile.getAbsolutePath();
    // Add it to the top of the pile.  If it exists already in the pile,
    // remove it from that list.  If not, drop the oldest of the bottom of
    // the pile
    if (absPaths_.contains(absPath)) {
      absPaths_.remove(absPath);
    } else {
      int numPaths = absPaths_.size();
      if (numPaths == HISTORY_DEPTH_) {
        String lastAbs = absPaths_.get(numPaths - 1);
        names_.remove(lastAbs);
        absPaths_.remove(numPaths - 1);
      }
    }
    names_.put(absPath, recentFile.getName());
    absPaths_.add(0, absPath);
       
    //
    // Commit the files to preferences
    //
    
    commitToPrefs();
    return;
  }  
  
  /***************************************************************************
  ** 
  ** Commit state to prefs
  */

  private void commitToPrefs() {
    if (appState_.isHeadless()) {
      return;
    }
    Preferences prefs = Preferences.userNodeForPackage(MainCommands.class);
    StringBuffer buf = new StringBuffer();
    int numPaths = absPaths_.size();
    for (int i = 0; i < numPaths; i++) {
      buf.setLength(0);
      buf.append("RecentFile");
      buf.append(i);
      prefs.put(buf.toString(), absPaths_.get(i));
    }
    for (int i = numPaths; i < HISTORY_DEPTH_; i++) {
      buf.setLength(0);
      buf.append("RecentFile");
      buf.append(i);
      prefs.remove(buf.toString());
    }
    return;
  }

  /***************************************************************************
  ** 
  ** Ask if state is available from prefs
  */

  private boolean canLoadFromPrefs() {
    if (appState_.isHeadless()) {
      return (false);
    }
    Preferences prefs = Preferences.userNodeForPackage(MainCommands.class);
    String filePath = prefs.get("RecentFile0", null);
    return (filePath != null);
  }  

  /***************************************************************************
  ** 
  ** Load state from prefs
  */

  private void loadFromPrefs() {
    if (appState_.isHeadless()) {
      return;
    }
    absPaths_.clear();
    names_.clear();
    Preferences prefs = Preferences.userNodeForPackage(MainCommands.class);
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < HISTORY_DEPTH_; i++) {
      buf.setLength(0);
      buf.append("RecentFile");
      buf.append(i);
      String filePath = prefs.get(buf.toString(), null);
      if (filePath == null) {
        break;
      }
      File recentFile = new File(filePath);
      absPaths_.add(filePath);
      names_.put(filePath, recentFile.getName());
    }  
  }
}
