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

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;

/****************************************************************************
**
** Handles undos of basic database contents (e.g. genomes, layouts);
*/

public class DatabaseChangeCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private DatabaseChange restore_;
  
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
  
  public DatabaseChangeCmd(DataAccessContext dacx, DatabaseChange restore) {
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
    return ("Database Change");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() {
    super.undo();
    
    if (restore_.dataSharingPolicyChange || restore_.sharedTimeAxisChange || restore_.sharedPertDataChange || 
        restore_.sharedTcdChange || restore_.sharedCPEChange) {
      dacx_.getMetabase().metabaseSharedChangeUndo(restore_);
    } else {
      dacx_.getMetabase().getDB(tSrc_.getCurrentTab()).databaseChangeUndo(restore_);
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
    if (restore_.dataSharingPolicyChange || restore_.sharedTimeAxisChange || restore_.sharedPertDataChange || 
        restore_.sharedTcdChange || restore_.sharedCPEChange) {
      dacx_.getMetabase().metabaseSharedChangeRedo(restore_);
    } else {
      dacx_.getMetabase().getDB(tSrc_.getCurrentTab()).databaseChangeRedo(restore_); 
    }
    return;
  }
}
