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

package org.systemsbiology.biotapestry.ui;

import java.util.Set;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;

/****************************************************************************
**
** Currently stocks fonts and Display Options; should also be used for COLORS  FIX ME
*/

public class GlobalDataFactory extends AbstractFactoryClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String fontKey_;
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

  public GlobalDataFactory(BTState appState) { 
    super(new FactoryWhiteboard());
    appState_ = appState;
    
    Set<String> fontMgrKeys = FontManager.keywordsOfInterest();
    fontKey_ = FontManager.getFontKeyword();        
    
    FactoryWhiteboard whiteboard = (FactoryWhiteboard)sharedWhiteboard_;
    AbstractFactoryClient dow = new DisplayOptions.DisplayOptionsWorker(appState_, whiteboard);
    installWorker(dow, null);
    // Kinda bogus, but we have no DisplayOptionsManager tag to work with, and no glue stick:
    myKeys_.addAll(dow.keywordsOfInterest());
 
    myKeys_.addAll(fontMgrKeys);
    myKeys_.add(fontKey_); 
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  protected Object localProcessElement(String elemName, Attributes attrs) throws IOException {

    if ((attrs == null) || (elemName == null)) {
      return (null);
    }  
    if (elemName.equals(fontKey_)) {
      FontManager.installFromXML(appState_, elemName, attrs);
      return (null);
    }
    return (null);
  }
}

