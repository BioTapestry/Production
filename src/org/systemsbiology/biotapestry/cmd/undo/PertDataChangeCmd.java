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
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PerturbationData;

/****************************************************************************
**
** Handles undos of perturbation changes
*/

public class PertDataChangeCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private PertDataChange restore_;
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
  
  public PertDataChangeCmd(BTState appState, DataAccessContext dacx, PertDataChange restore) {
    super(appState, dacx);
    restore_ = restore;
    doEvent_ = false;
  }  
  
  /***************************************************************************
  **
  ** Build the command
  */ 
  
  public PertDataChangeCmd(BTState appState, DataAccessContext dacx, PertDataChange restore, boolean doEvent) {
    super(appState, dacx);
    restore_ = restore;
    doEvent_ = doEvent;
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
    return ("Pert Change");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() {
    super.undo();
    PerturbationData pd = dacx_.getExpDataSrc().getPertData();
    pd.changeUndo(restore_);
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
    PerturbationData pd = dacx_.getExpDataSrc().getPertData();
    pd.changeRedo(restore_);
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
  
  public static PertDataChangeCmd[] wrapChanges(BTState appState, DataAccessContext dacx, PertDataChange[] restores) {
    PertDataChangeCmd[] retval = new PertDataChangeCmd[restores.length];
    for (int i = 0; i < retval.length; i++) {
      retval[i] = new PertDataChangeCmd(appState, dacx, restores[i]);
    }
    return (retval);
  }  
}
