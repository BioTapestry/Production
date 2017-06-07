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

package org.systemsbiology.biotapestry.cmd.instruct;

import java.util.Set;
import java.util.HashSet;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** This builds Instance Instruction Sets
*/

public class InstanceInstructionSetFactory implements ParserClient {

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
  
  private InstanceInstructionSet currSet_;
   
  private Set<String> setKeys_;
  private String regionKey_;
  private Set<String> instructionKeys_;
  private HashSet<String> allKeys_;
  private DataAccessContext dacx_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor for an InstanceInstructionSet factory
  */

  public InstanceInstructionSetFactory() {
    setKeys_ = InstanceInstructionSet.keywordsOfInterest();
    instructionKeys_ = BuildInstructionInstance.keywordsOfInterest();    
    regionKey_ = InstanceInstructionSet.regionKeyword();

    allKeys_ = new HashSet<String>();
    allKeys_.addAll(setKeys_);
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

  public void setContext(DataAccessContext dacx) {
    dacx_ = dacx;
    return;
  }

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
    if (setKeys_.contains(elemName)) {
      InstanceInstructionSet iis = InstanceInstructionSet.buildFromXML(elemName, attrs);
      if (iis != null) {
        dacx_.getInstructSrc().addInstanceInstructionSet(iis.getInstanceID(), iis);
        currSet_ = iis;
        return (iis);
      }
    } else if (instructionKeys_.contains(elemName)) {
      BuildInstructionInstance bii = BuildInstructionInstance.buildFromXML(elemName, attrs);
      if (bii != null) {
        currSet_.addInstruction(bii);
      }
    } else if (regionKey_.equals(elemName)) {
      currSet_.addRegion(InstanceInstructionSet.extractRegion(elemName, attrs));
    }

    return (null);
  }
}
