/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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

import javax.swing.undo.AbstractUndoableEdit;

import org.systemsbiology.biotapestry.app.CmdSource;
import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** Base class for undo commands
*/

public abstract class BTUndoCmd extends AbstractUndoableEdit {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  protected UIComponentSource uics_;
  protected StaticDataAccessContext dacx_;
  protected CmdSource cSrc_;
  protected TabSource tSrc_;
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** DataAccess now arrives from Support framework
  */ 
   
  public BTUndoCmd() {
  }
 
  /***************************************************************************
  **
  ** Migrate away from constructor now!
  */ 
   
  public BTUndoCmd(DataAccessContext dacx) {
    dacx_ = new StaticDataAccessContext(dacx);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** We can add the app state when the command is registered with support. This
  ** allows us to avoid needing to pass appState around so much!
  */ 
  
  public void setAppState(TabSource tSrc, CmdSource cSrc, UIComponentSource uics, StaticDataAccessContext dacx) {
    tSrc_ = tSrc;
    uics_ = uics;
    cSrc_ = cSrc;
    dacx_ = dacx;
    return;
  }

  /***************************************************************************
  **
  ** Answer if the command has permanent effects
  */ 
  
  public boolean changesModel() {
    return (true);
  }
}
