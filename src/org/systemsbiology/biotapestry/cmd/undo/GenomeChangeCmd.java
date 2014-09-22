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
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;

/****************************************************************************
**
** Handles undos of genome changes
*/

public class GenomeChangeCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private GenomeChange restore_;
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
  
  public GenomeChangeCmd(BTState appState, DataAccessContext dacx, GenomeChange restore) {
    this(appState, dacx, restore, false);
  }

  /***************************************************************************
  **
  ** Build the command
  */ 
  
  public GenomeChangeCmd(BTState appState, DataAccessContext dacx, GenomeChange restore, boolean doEvent) {
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
    return ("Model Change");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() {
    super.undo();
    Genome genome = dacx_.getGenomeSource().getGenome(restore_.genomeKey);
    genome.changeUndo(restore_);
    if (doEvent_) {
      ModelChangeEvent ev = new ModelChangeEvent(restore_.genomeKey, ModelChangeEvent.UNSPECIFIED_CHANGE);
      appState_.getEventMgr().sendModelChangeEvent(ev); 
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
    Genome genome = dacx_.getGenomeSource().getGenome(restore_.genomeKey);
    genome.changeRedo(restore_);
    if (doEvent_) {
      ModelChangeEvent ev = new ModelChangeEvent(restore_.genomeKey, ModelChangeEvent.UNSPECIFIED_CHANGE);
      appState_.getEventMgr().sendModelChangeEvent(ev); 
    }
    return;
  }
}
