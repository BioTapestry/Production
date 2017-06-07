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

package org.systemsbiology.biotapestry.app;

import java.io.File;

import org.systemsbiology.biotapestry.nav.RecentFilesManager;

/****************************************************************************
**
** Working to get the kitchen-sink dependency on BTState object across the
** program. This provides info on files and directories
*/

public class PathAndFileSource { 
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private BTState appState_;
  private File serverBtpDirectory_;
  private File serverConfigDirectory_;
  private String modelListFile_;
  private String fullServletContextPath_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  PathAndFileSource(BTState appState) {
    appState_ = appState;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  ** 
  ** Get the server BTP directory
  */
  
  public File getServerBtpDirectory() {
    return (serverBtpDirectory_);  
  } 
  
  /***************************************************************************
  ** 
  ** Get the server config files directory
  */
  
  public File getServerConfigDirectory() {
    return (serverConfigDirectory_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the server BTP directory
  */
  
  public void setServerBtpDirectory(String dir) {
    serverBtpDirectory_ = new File(dir); 
    return;
  } 
  
  /***************************************************************************
  ** 
  ** Set the server config files directory
  */
  
  public void setServerConfigDirectory(String dir) {
    serverConfigDirectory_ = new File(dir); 
    return;
  } 
 
  /***************************************************************************
  ** 
  ** Get the servlet's context path (needed to access resource files)
  *
  */  
  
  public String getFullServletContextPath() {
    return (fullServletContextPath_);
  }

  /***************************************************************************
  ** 
  ** Set the servlet's context path (needed to access resource files)
  *
  */
  
  public void setFullServletContextPath(String fullServletContextPath) {
    fullServletContextPath_ = fullServletContextPath;
    return;
  }

  /***************************************************************************
  ** 
  ** Get the list of valid model files (if applicable)
  */
  
  public String getServerBtpFileList() {
    return (modelListFile_);  
  }   
  
  /***************************************************************************
  ** 
  ** Set the list of valid model files (if applicable)
  */
  
  public void setServerBtpFileList(String modelListFile) {
    modelListFile_ = modelListFile;
    return;
  } 
  
  /***************************************************************************
  **
  ** Command
  */ 
      
  public RecentFilesManager getRecentFilesMgr() {
    return (appState_.getRecentFilesMgr());  
  }
   
  /****************************************************************************
  **
  ** Get current file
  */  
   
  public File getCurrentFile() {
    return (appState_.getCurrentFile());
  } 
  
 /****************************************************************************
  **
  ** Set current file
  */  
  
  public void setCurrentFile(File currentFile) {
    appState_.setCurrentFile(currentFile);
    return;
  }
}
