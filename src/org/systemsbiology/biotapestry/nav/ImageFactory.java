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

import java.util.Set;
import java.util.HashSet;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.parser.ParserClient;
import org.systemsbiology.biotapestry.app.BTState;


/****************************************************************************
**
** This builds Images into the image library
*/

public class ImageFactory implements ParserClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  private static final int NONE_  = 0;  
  private static final int IMAGE_ = 1;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private String currImage_;
  private int charTarget_;
  
  private Set<String> imageKeys_;
  private String imageKey_; 
  
  private HashSet<String> allKeys_;
  
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor for a genome factory
  */

  public ImageFactory(BTState appState) {
    appState_ = appState;
    imageKeys_ = ImageManager.keywordsOfInterest();
    imageKey_ = ImageManager.getImageKeyword();        
        
    allKeys_ = new HashSet<String>();
    allKeys_.addAll(imageKeys_);
   // allKeys_.add(imageKey_);
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
    charTarget_ = NONE_;
    if (currImage_ != null) {
      appState_.getImageMgr().finishImageDefinition(currImage_);
      currImage_ = null;
    }
    return (allKeys_.contains(elemName));
  }
  
  /***************************************************************************
  ** 
  ** Handle incoming characters
  */

  public void processCharacters(char[] chars, int start, int length) {
    switch (charTarget_) {
      case NONE_:
        break;
      case IMAGE_:
        appState_.getImageMgr().appendToImageDefinition(currImage_, chars, start, length);
        break;
      default:
        throw new IllegalStateException();
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
    if (imageKeys_.contains(elemName)) {
      return (null);  // Don't do anything with collection as a whole
    } else if (imageKey_.equals(elemName)) {
      currImage_ = ImageManager.installFromXML(appState_, elemName, attrs);
      charTarget_ = IMAGE_;
    }
    return (null);
  }
}
