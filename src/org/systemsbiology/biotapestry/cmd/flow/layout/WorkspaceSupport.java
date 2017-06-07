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

package org.systemsbiology.biotapestry.cmd.flow.layout;

import java.awt.Rectangle;

import org.systemsbiology.biotapestry.cmd.undo.DatabaseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DatabaseChange;
import org.systemsbiology.biotapestry.db.Workspace;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Handle centering current model
*/

public class WorkspaceSupport {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private DataAccessContext dacx_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public WorkspaceSupport(DataAccessContext dacx) {
    dacx_ = dacx;
  }

  /***************************************************************************
  **
  ** Set workspace
  */
    
  public void setWorkspace(Rectangle rect, UndoSupport support) {
    Workspace ws = new Workspace(rect);
    DatabaseChange dc = dacx_.getWorkspaceSource().setWorkspace(ws);
    if (dc != null) {
      DatabaseChangeCmd dcc = new DatabaseChangeCmd(dacx_, dc);
      support.addEdit(dcc);
      support.addEvent(new LayoutChangeEvent(dacx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));  
      support.finish();
    }
    return;
  }

  /***************************************************************************
  **
  ** Set workspace to bound the model
  */
  
  public void setWorkspaceToModelBounds(UndoSupport support, Rectangle allBounds) {
    Workspace boundWs = Workspace.setToModelBounds(allBounds);
    DatabaseChange dc = dacx_.getWorkspaceSource().setWorkspace(boundWs);
    if (dc != null) {
      DatabaseChangeCmd dcc = new DatabaseChangeCmd(dacx_, dc);
      support.addEdit(dcc);
      support.addEvent(new LayoutChangeEvent(dacx_.getCurrentLayoutID(), LayoutChangeEvent.UNSPECIFIED_CHANGE));  
    }
    return;
  }

}
