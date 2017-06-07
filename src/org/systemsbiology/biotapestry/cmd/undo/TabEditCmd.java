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

package org.systemsbiology.biotapestry.cmd.undo;

import org.systemsbiology.biotapestry.app.TabChange;
import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** Handles undos/redos of tab settings edits (title, desc, etc.)
*/

public class TabEditCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private TabChange restore_;
  private static final long serialVersionUID = 1L;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Build the command
  */ 
  
  public TabEditCmd(DataAccessContext dacx, TabChange restore) {
    super(dacx);
    restore_ = restore;
  }  
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Name to show
  */ 
  
  @Override
  public String getPresentationName() {
    return ("Tab Edit");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() {
    super.undo();
    // The PerTab variables might be null if this was a CommonView- or BTState-specific
    // undo, so check before we undo
    if(restore_.oldPerTab != null) {
    	uics_.getCommonView().editTabUndo(restore_.oldPerTab);
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo the operation
  */ 
  
  @Override
  public void redo() {
    super.redo();
    // The PerTab variables might be null if this was a CommonView- or BTState-specific
    // undo, so check before we redo
    if(restore_.newPerTab != null) {
    	uics_.getCommonView().editTabRedo(restore_.newPerTab);
    }
    return;
  }
}
