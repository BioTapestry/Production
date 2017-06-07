/*
**    Copyright (C) 2003-2009 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.plugin;

import java.util.Set;
import java.util.HashSet;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.parser.ParserClient;

/****************************************************************************
**
** This builds PluginDirectives from an XML file
*/

public class PlugInDirectiveFactory implements ParserClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private Set<String> legacyKeys_;
  private Set<String>instKeys_;
  private Set<String> linkInstKeys_;
  private Set<String> simKeys_;
  private Set<String> mbKeys_;
  private HashSet<String> allKeys_;
  private PlugInManager mgr_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor for the plugin directive factory
  */

  public PlugInDirectiveFactory(PlugInManager mgr) {
    legacyKeys_ = PlugInDirective.keywordsOfInterest();
    instKeys_ = NodePlugInDirective.keywordsOfInterest();
    linkInstKeys_ = LinkPlugInDirective.keywordsOfInterest();
    simKeys_ = SimulatorPlugInDirective.keywordsOfInterest();
    mbKeys_ = ModelBuilderPlugInDirective.keywordsOfInterest();
    allKeys_ = new HashSet<String>(instKeys_);
    allKeys_.addAll(linkInstKeys_);
    allKeys_.addAll(legacyKeys_);
    allKeys_.addAll(simKeys_);
    allKeys_.addAll(mbKeys_);
    mgr_ = mgr;
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
    return;
  }
  
  /***************************************************************************
  **
  ** Callback for completion of the element
  **
  */
  
  public boolean finishElement(String elemName) {
    return (allKeys_.contains(elemName));
  }
  
  /***************************************************************************
  ** 
  ** Handle incoming characters
  */

  public void processCharacters(char[] chars, int start, int length) {
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

    if (legacyKeys_.contains(elemName)) {
      PlugInDirective pid = PlugInDirective.buildFromXML(elemName, attrs);
      if (pid != null) {
        mgr_.addDirective(pid);
        return (pid);
      }
    } else if (instKeys_.contains(elemName)) {
      NodePlugInDirective pid = NodePlugInDirective.buildFromXML(elemName, attrs);
      if (pid != null) {
        mgr_.addDirective(pid);
        return (pid);
      }
    } else if (linkInstKeys_.contains(elemName)) {
      LinkPlugInDirective pid = LinkPlugInDirective.buildFromXML(elemName, attrs);
      if (pid != null) {
        mgr_.addLinkDirective(pid);
        return (pid);
      }
    } else if (simKeys_.contains(elemName)) {
      SimulatorPlugInDirective pid = SimulatorPlugInDirective.buildFromXML(elemName, attrs);
      if (pid != null) {
        mgr_.addSimDirective(pid);
        return (pid);
      }
    } else if (mbKeys_.contains(elemName)) {
      ModelBuilderPlugInDirective pid = ModelBuilderPlugInDirective.buildFromXML(elemName, attrs);
      if (pid != null) {
        mgr_.addModelBuilderDirective(pid);
        return (pid);
      }
    } 
    return (null);
  }
}
