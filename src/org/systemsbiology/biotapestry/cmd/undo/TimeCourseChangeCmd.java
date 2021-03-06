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

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;

/****************************************************************************
**
** Handles undos of time course changes
*/

public class TimeCourseChangeCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private TimeCourseChange restore_;
  private boolean doEvent_;
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
  
  public TimeCourseChangeCmd(BTState appState, DataAccessContext dacx, TimeCourseChange restore, boolean doEvent) {
    super(appState, dacx);
    restore_ = restore;
    doEvent_ = doEvent;
  }
  
  /***************************************************************************
  **
  ** Build the command
  */ 
  
  public TimeCourseChangeCmd(BTState appState, DataAccessContext dacx, TimeCourseChange restore) {
    this(appState, dacx, restore, false);
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
    return ("Time Course Change");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() {
    super.undo();
    TimeCourseData tcd = appState_.getDB().getTimeCourseData();
    tcd.changeUndo(restore_);
    if (doEvent_) {
      GeneralChangeEvent ev = new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE);
      appState_.getEventMgr().sendGeneralChangeEvent(ev); 
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
    TimeCourseData tcd = appState_.getDB().getTimeCourseData();
    tcd.changeRedo(restore_);
    if (doEvent_) {
      GeneralChangeEvent ev = new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE);
      appState_.getEventMgr().sendGeneralChangeEvent(ev); 
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Wrap changes in command array
  */ 
  
  public static TimeCourseChangeCmd[] wrapChanges(BTState appState, DataAccessContext dacx, TimeCourseChange[] restores) {
    TimeCourseChangeCmd[] retval = new TimeCourseChangeCmd[restores.length];
    for (int i = 0; i < retval.length; i++) {
      retval[i] = new TimeCourseChangeCmd(appState, dacx, restores[i]);
    }
    return (retval);
  }

}
