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


package org.systemsbiology.biotapestry.embedded;

import org.systemsbiology.biotapestry.genome.DBLinkage;

/****************************************************************************
**
** Contains external inventory info for a user of an embedded BioTapestry
** panel
*/

public class ExternalInventoryLink extends ExternalInventoryItem {
  
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
 
  private ArgsForExternalLink afel_;
  private int sign_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the info
  */ 
  
  public ExternalInventoryLink(BuilderArgs bArgs, ArgsForExternalLink afel, int sign) {
    super(bArgs);
    afel_ = afel;
    sign_ = sign;
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
    return (afel_.modelNameChain);
  }
  
  /***************************************************************************
  **
  ** Get the link source name
  */ 
  
  public String getSourceNodeName() {
    return (afel_.srcNodeName);
  }
  
  /***************************************************************************
  **
  ** Get the link target name
  */ 
  
  public String getTargetNodeName() {
    return (afel_.trgNodeName);
  }

  /***************************************************************************
  **
  ** Get the link source region name
  */ 
  
  public String getSourceRegionName() {
    return (afel_.srcRegionName);
  }
  
  /***************************************************************************
  **
  ** Get the link target region name
  */ 
  
  public String getTargetRegionName() {
    return (afel_.trgRegionName);
  }
  
  /***************************************************************************
  **
  ** Get the display (w/o type tags)
  */ 
  
  public String getSimpleDisplay() {
    return (afel_.simpleLinkDisplay);
  }
    
  /***************************************************************************
  **
  ** Get the display (with type tags)
  */ 
  
  public String getDisplay() {
    return (afel_.linkDisplay);
  } 
    
  /***************************************************************************
  **
  ** Get the sign: "negative", "neutral", or "positive" 
  */ 
  
  public String getSign() {
    return (DBLinkage.mapSignToDisplay(bArgs_.appState, sign_));
  } 

  /***************************************************************************
  **
  ** Display string
  */ 
  
  public String toString() {
    return (getDisplay());
  }  
}
