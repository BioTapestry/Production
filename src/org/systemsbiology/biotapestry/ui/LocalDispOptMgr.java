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

package org.systemsbiology.biotapestry.ui;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Limited Dummy Display Options Manager.
*/

public class LocalDispOptMgr implements MinimalDispOptMgr {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private DisplayOptions options_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** constructor
  */
    
  public LocalDispOptMgr(DisplayOptions dopt) {
    options_ = dopt;
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  ** 
  ** Get the display options
  */

  public DisplayOptions getDisplayOptions() {
    return (options_);
  }
  
  /***************************************************************************
  **
  ** Turn on special link branches
  ** 
  */
  
  public void turnOnSpecialLinkBranches(UndoSupport support, DataAccessContext dacx, boolean doFinish) {     
    options_.setBranchMode(DisplayOptions.OUTLINED_BUS_BRANCHES);
    return;
  }
  
  public void changeUndo(DisplayOptionsChange doc) {
    throw new UnsupportedOperationException();
    
  }
  
    public void changeRedo(DisplayOptionsChange doc) {
    throw new UnsupportedOperationException();
    
  }
    
  public void setDisplayOptionsForIO(DisplayOptions dop) {
    throw new UnsupportedOperationException();  
  }  
  
  public void modifyForPertDataChange(UndoSupport sup, DataAccessContext dac) {
    throw new UnsupportedOperationException();
    
  }
      
  public DisplayOptionsChange setDisplayOptions(DisplayOptions dop) {
    throw new UnsupportedOperationException();
  }
}
