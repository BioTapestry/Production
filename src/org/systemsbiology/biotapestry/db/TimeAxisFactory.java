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

import java.util.Set;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;

/****************************************************************************
**
** This builds time axis.
*/

public class TimeAxisFactory extends AbstractFactoryClient {

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
  
  private DataAccessContext dacx_;    
  private Set<String> timeAxisKeys_;
  private String timeAxisStageKey_;
  private boolean isForMeta_;
  private TimeAxisDefinition currTarg_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor for the model data factory
  */

  public TimeAxisFactory(FactoryWhiteboard whiteboard, boolean isForMeta) {
    super(whiteboard);
    isForMeta_ = isForMeta;
    timeAxisKeys_ = TimeAxisDefinition.keywordsOfInterest();
    timeAxisStageKey_ = TimeAxisDefinition.getStageKeyword();      
    myKeys_.addAll(timeAxisKeys_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Set the context
  **
  */
  
  public void setContext(DataAccessContext dacx) {
    dacx_ = dacx;
    return;
  }
  
  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  public Object localProcessElement(String elemName, Attributes attrs) throws IOException { 
    if (timeAxisKeys_.contains(elemName)) {
      TimeAxisDefinition tad = TimeAxisDefinition.buildFromXML(dacx_, elemName, attrs);
      if (tad != null) {
        currTarg_ = tad;        
        if (isForMeta_) {
          dacx_.getMetabase().setSharedTimeAxisDefinition(tad);
        } else {
          dacx_.getLocalDataCopyTarget().installLocalTimeAxisDefinition(tad);
        }
      }
    } else if (timeAxisStageKey_.equals(elemName)) {
      TimeAxisDefinition.NamedStage ns = TimeAxisDefinition.NamedStage.buildFromXML(elemName, attrs);
      if (ns != null) {
        currTarg_.addAStage(ns);
      }
    }
    return (null);
  }
  
  /***************************************************************************
  **
  ** Callback for completion of the element
  **
  */
  
  @Override
  public void localFinishElement(String elemName) throws IOException {
    currTarg_ = null;
    return;
  }
}
