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

package org.systemsbiology.biotapestry.db;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** This handles reading from XML files: we need to route to legacy or multi-tab processing via Metabase
*/

public class MasterFactory implements ParserClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private HashSet<String> allKeys_; 
  private ParserClient currClient_;
  private boolean isViewer_;
  private boolean isForAppend_;
  private boolean legacyFactory_;
  private UndoSupport supportForAppend_;
   
  private DynamicDataAccessContext ddacx_;
  private UIComponentSource uics_;
  private TabSource tSrc_;
  private UndoFactory uFac_;
  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public MasterFactory(boolean isViewer, boolean isForAppend, UndoSupport forAppend,     
                       DynamicDataAccessContext ddacx, UIComponentSource uics, 
                       TabSource tSrc, UndoFactory uFac) {
    
    ddacx_ = ddacx;
    uics_ = uics;
    tSrc_ = tSrc;
    uFac_ = uFac;
    if (isForAppend != (forAppend != null)) {
      throw new IllegalArgumentException();
    }
    // the only tag we advertise about is the biotapestry tag:;
    supportForAppend_ = forAppend;
    isViewer_ = isViewer;
    isForAppend_ = isForAppend;
    allKeys_ = new HashSet<String>();
    allKeys_.add("BioTapestry");
    currClient_ = null;
    legacyFactory_ = false;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get list of merge issues
  */

  public List<String> getMergeIssues() {
    if (legacyFactory_) {
     return (((DatabaseFactory)currClient_).getMergeErrors()); 
    } else {
     return (((MetabaseFactory)currClient_).getMergeErrors()); 
    }
  }
 
  /***************************************************************************
  ** 
  ** Set the container
  */

  public void setContainer(Object container) {
    if (currClient_ != null) {
      currClient_.setContainer(container);
    }    
    return;    
  }
    
  /***************************************************************************
  **
  ** Callback for completion of the element
  **
  */
  
  public boolean finishElement(String elemName) throws IOException {
    if (currClient_ == null) {
      return (false);
    }
    currClient_.finishElement(elemName);
    return (allKeys_.contains(elemName));
  }
  
  /***************************************************************************
  ** 
  ** Handle incoming characters
  */

  public void processCharacters(char[] chars, int start, int length) {
    if (currClient_ != null) {
      currClient_.processCharacters(chars, start, length);
    }    
    return;
  }
  
  /***************************************************************************
  **
  ** Return the element keywords that we are interested in
  **
  */
  
  public Set<String> keywordsOfInterest() {
    return (allKeys_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public Object processElement(String elemName, Attributes attrs) throws IOException {

    if (allKeys_.contains(elemName)) {
      String version = Metabase.versionFromXML(elemName, attrs);
      ddacx_.getMetabase().setIOVersion(version);
      if (Metabase.preMetabaseVersion(version)) {
        currClient_ = new DatabaseFactory(isViewer_, isForAppend_, supportForAppend_, ddacx_, uics_, tSrc_, uFac_);
        legacyFactory_ = true;
      } else {
        currClient_ = new MetabaseFactory(isViewer_, isForAppend_, supportForAppend_, ddacx_, uics_, tSrc_, uFac_);
      }      
      return (null);
    }
    
    return (currClient_.processElement(elemName, attrs));
  }
}

