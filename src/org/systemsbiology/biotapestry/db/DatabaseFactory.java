/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstructionFactory;
import org.systemsbiology.biotapestry.cmd.instruct.InstanceInstructionSetFactory;
import org.systemsbiology.biotapestry.genome.DBGenomeFactory;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxyFactory;
import org.systemsbiology.biotapestry.genome.GenomeInstanceFactory;
import org.systemsbiology.biotapestry.nav.ImageFactory;
import org.systemsbiology.biotapestry.nav.UserTreePathFactory;
import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.qpcr.QpcrLegacyPublicExposed;
import org.systemsbiology.biotapestry.timeCourse.CopiesPerEmbryoFormatFactory;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeFormatFactory;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseFormatFactory;
import org.systemsbiology.biotapestry.ui.GlobalDataFactory;
import org.systemsbiology.biotapestry.ui.LayoutFactory;

/****************************************************************************
**
** This handles database creation from XML files
*/

public class DatabaseFactory implements ParserClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private Set bioTapHeaderKeys_; 
  private HashSet allKeys_; 
  private ParserClient currClient_;
  private HashMap<String, ParserClient> clients_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public DatabaseFactory(BTState appState, boolean isViewer) {
      
    // the only tag we advertise about is the biotapestry tag:
    appState_ = appState;
    bioTapHeaderKeys_ = Database.keywordsOfInterest();  
    allKeys_ = new HashSet();
    allKeys_.addAll(bioTapHeaderKeys_);
    DataAccessContext dacx = new DataAccessContext(appState_);
    
    ArrayList<ParserClient> alist = new ArrayList<ParserClient>();
    alist.add(new DBGenomeFactory(appState_, dacx));
    alist.add(new GenomeInstanceFactory(appState_, dacx));
    alist.add(new LayoutFactory(appState_)); 
    alist.add(new ModelDataFactory(appState_));
    alist.add(new BuildInstructionFactory(appState_));
    alist.add(new InstanceInstructionSetFactory(appState_));
    alist.add(new GlobalDataFactory(appState_));
    alist.add(new ImageFactory(appState_));
    alist.add(new UserTreePathFactory(appState_));    
    alist.add(new QpcrLegacyPublicExposed(appState_).getParserClient(false, false)); 
    alist.add(new TimeCourseFormatFactory(appState_, false, false));
    alist.add(new CopiesPerEmbryoFormatFactory(appState_, false));
    alist.add(new TemporalInputRangeFormatFactory(appState_, false));
    alist.add(new DynamicInstanceProxyFactory(appState_, dacx));
    alist.add(new PerturbationData.PertDataWorker(appState_, false, false));
    
    Iterator<ParserClient> cit = alist.iterator();
    clients_ = new HashMap<String, ParserClient>();
    while (cit.hasNext()) {
      ParserClient pc = cit.next();
      Set<String> keys = pc.keywordsOfInterest();
      Iterator<String> ki = keys.iterator();
      while (ki.hasNext()) {
        String key = ki.next();
        Object prev = clients_.put(key, pc);
        if (prev != null) {
          throw new IllegalArgumentException();
        }
      }
    }
    currClient_ = null;
    
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

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
    if (currClient_.finishElement(elemName)) {
      currClient_ = null;
    }
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
  
  public Set keywordsOfInterest() {
    return (allKeys_);
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public Object processElement(String elemName, Attributes attrs) throws IOException {

    if (bioTapHeaderKeys_.contains(elemName)) {
      String version = Database.versionFromXML(elemName, attrs);
      appState_.getDB().setIOVersion(version);
      return (null);
    }
    
    if (currClient_ != null) {
      return (currClient_.processElement(elemName, attrs));
    }
    
    ParserClient pc = (ParserClient)clients_.get(elemName);
    if (pc != null) {
      currClient_ = pc; 
      return (currClient_.processElement(elemName, attrs));
    }
    return (null);
  }
}

