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
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;

/****************************************************************************
**
** Handles undos of temporal input changes
*/

public class TemporalInputChangeCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private TemporalInputChange restore_;
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
  
  public TemporalInputChangeCmd(DataAccessContext dacx, TemporalInputChange restore, boolean doEvent) {
    super(dacx);
    restore_ = restore;
    doEvent_ = doEvent;
  }
  
  /***************************************************************************
  **
  ** Build the command
  */ 
  
  public TemporalInputChangeCmd(DataAccessContext dacx, TemporalInputChange restore) {
    this(dacx, restore, false);
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
    return ("Temporal Input Change");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
 
  @Override
  public void undo() {
    super.undo();
    TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
    tird.changeUndo(restore_);
    if (doEvent_) {
      GeneralChangeEvent ev = new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE);
      uics_.getEventMgr().sendGeneralChangeEvent(ev); 
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
    TemporalInputRangeData tird = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
    tird.changeRedo(restore_);
    if (doEvent_) {
      GeneralChangeEvent ev = new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE);
      uics_.getEventMgr().sendGeneralChangeEvent(ev); 
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
  
  public static TemporalInputChangeCmd[] wrapChanges(DataAccessContext dacx, TemporalInputChange[] restores) {
    TemporalInputChangeCmd[] retval = new TemporalInputChangeCmd[restores.length];
    for (int i = 0; i < retval.length; i++) {
      retval[i] = new TemporalInputChangeCmd(dacx, restores[i]);
    }
    return (retval);
  }

}
