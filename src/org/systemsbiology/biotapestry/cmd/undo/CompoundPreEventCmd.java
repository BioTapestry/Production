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

import java.util.ArrayList;
import java.util.List;

import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ChangeEvent;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.event.SelectionChangeEvent;
import org.systemsbiology.biotapestry.event.TreeNodeChangeEvent;
import org.systemsbiology.biotapestry.event.EventManager;
import org.systemsbiology.biotapestry.event.OverlayDisplayChangeEvent;
import org.systemsbiology.biotapestry.modelBuild.ModelBuilder;

/****************************************************************************
**
** Does eventing following Compound Editing undo
*/

public class CompoundPreEventCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<ChangeEvent> ev_;
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
  
  public CompoundPreEventCmd(DataAccessContext dacx) {
    super(dacx);
    ev_ = new ArrayList<ChangeEvent>();
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Add the events
  */ 
  
  public void addChangeEvents(List<ChangeEvent> events) {
    ev_.addAll(events);
    return;
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
    return ("PreEvent");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() {
    super.undo();
    EventManager mgr = uics_.getEventMgr();
    int num = ev_.size();
    int numMCE = 0;
    for (int i = 0; i < num; i++) {
      ChangeEvent ce = ev_.get(i);
      if (ce instanceof ModelChangeEvent) {
        numMCE++;
      }
    } 
    
    //
    // For better performance, some listeners who pay no attention to
    // the contents of a model change only care about last event:
    //
 
    for (int i = num - 1; i >= 0; i--) {
      ChangeEvent ce = ev_.get(i);
      if (ce instanceof GeneralChangeEvent) {
        mgr.sendGeneralChangeEvent((GeneralChangeEvent)ce);
      } else if (ce instanceof LayoutChangeEvent) {
        mgr.sendLayoutChangeEvent((LayoutChangeEvent)ce);
      } else if (ce instanceof ModelChangeEvent) {
        mgr.sendModelChangeEvent((ModelChangeEvent)ce, --numMCE);
      } else if (ce instanceof SelectionChangeEvent) {
        mgr.sendSelectionChangeEvent((SelectionChangeEvent)ce);
      } else if (ce instanceof OverlayDisplayChangeEvent) {
        mgr.sendOverlayDisplayChangeEvent((OverlayDisplayChangeEvent)ce);
      } else if (ce instanceof ModelBuilder.MBChangeEvent) {
        mgr.sendModelBuildChangeEvent((ModelBuilder.MBChangeEvent)ce);   
      } else if (ce instanceof TreeNodeChangeEvent) {
        mgr.sendTreeNodeChangeEvent((TreeNodeChangeEvent)ce);       
        
        
      } else {
        throw new IllegalArgumentException();
      }
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
    return;
  }
}
