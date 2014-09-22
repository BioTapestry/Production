/*
**    Copyright (C) 2003-2011 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.embedded;

import java.util.ArrayList;
import java.util.List;

import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;

/****************************************************************************
**
** Contains external inventory info for a user of an embedded BioTapestry
** panel
*/

public class ExternalInventoryNode extends ExternalInventoryItem {
  
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
  
  private ArgsForExternalNode afen_;
  private ArrayList<TimeCourseData.TCMapping> timeCourseMaps_;
  private boolean internal_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the info
  */ 
  
  public ExternalInventoryNode(BuilderArgs bArgs, ArgsForExternalNode afen, 
                               List<TimeCourseData.TCMapping> timeCourse, boolean internalOnly) {
    super(bArgs);
    afen_ = afen;
    timeCourseMaps_ = (timeCourse == null) ? null : new ArrayList<TimeCourseData.TCMapping>(timeCourse);
    internal_ = internalOnly;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Get the model name chain
  */ 
  
  public String[] getModelNameChain() {
    return (afen_.modelNameChain);
  }
  
  /***************************************************************************
  **
  ** Get the node name
  */ 
  
  public String getNodeName() {
    return (afen_.nodeName);
  }  
  
  /***************************************************************************
  **
  ** Get the node display (w/o type tag)
  */ 
  
  public String getSimpleNodeDisplay() {
    return (afen_.simpleNodeDisplay);
  }
    
  /***************************************************************************
  **
  ** Get the node display (with type tag)
  */ 
  
  public String getNodeDisplay() {
    return (afen_.nodeDisplay);
  }
  
  /***************************************************************************
  **
  ** Get mapped names (may be null)
  */ 
  
  public List<TimeCourseData.TCMapping> getMappedNames() {
    return ((timeCourseMaps_ == null) ? null : new ArrayList<TimeCourseData.TCMapping>(timeCourseMaps_));
  }
  
  /***************************************************************************
  **
  ** Answer if time course data is tagged as "internal only"
  */ 
  
  public boolean isInternalOnly() {
    return (internal_);
  }
  
  /***************************************************************************
  **
  ** Display string
  */ 
  
  public String toString() {
    return (getNodeDisplay());
  }  
}
