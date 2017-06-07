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

package org.systemsbiology.biotapestry.nav;

import java.util.Set;
import java.io.IOException;

import org.xml.sax.Attributes;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.genome.FactoryWhiteboard;
import org.systemsbiology.biotapestry.parser.AbstractFactoryClient;
import org.systemsbiology.biotapestry.parser.GlueStick;

/****************************************************************************
**
** This builds tree paths into the tree path library
*/

public class UserTreePathFactory extends AbstractFactoryClient {
  
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

  private Set<String> mgrKeys_;
  private MyGlue mGlue_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor for a user path factory
  */

  public UserTreePathFactory() {
    super(new FactoryWhiteboard());
    mgrKeys_ = UserTreePathManager.keywordsOfInterest();
    FactoryWhiteboard whiteboard = (FactoryWhiteboard)sharedWhiteboard_;
    mGlue_ = new MyGlue();
    installWorker(new UserTreePath.UserTreePathWorker(whiteboard), mGlue_);
    myKeys_.addAll(mgrKeys_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Set context
  **
  */  
  
  public void setContext(UIComponentSource uics) {
    mGlue_.setContext(uics);
    return;
  }

  /***************************************************************************
  **
  ** Handle the attributes for the keyword
  **
  */
  
  protected Object localProcessElement(String elemName, Attributes attrs) throws IOException { 
    return (null);
  }

  /***************************************************************************
  **
  ** Adds paths to the path manager
  **
  */  
  
  public static class MyGlue implements GlueStick {
    
    private UIComponentSource uics_;
    
    public MyGlue() {
    } 
    
    public void setContext(UIComponentSource uics) {
      uics_ = uics;
      return;
    }
    
    public Object glueKidToParent(Object kidObj, AbstractFactoryClient parentWorker, 
                                  Object optionalArgs) throws IOException {
      FactoryWhiteboard board = (FactoryWhiteboard)optionalArgs;
      UserTreePathManager mgr = uics_.getPathMgr();
      UserTreePath path = board.userTreePath;
      mgr.addPreexistingPath(path);
      return (null);
    }
  } 
}
