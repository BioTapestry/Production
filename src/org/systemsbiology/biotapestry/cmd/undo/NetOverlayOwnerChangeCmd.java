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
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.NetworkOverlayOwnerChange;

/****************************************************************************
**import org.systemsbiology.biotapestry.genome.Genome;
** Handles undos of net overlay ownership changes
*/

public class NetOverlayOwnerChangeCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private NetworkOverlayOwnerChange restore_;
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
  
  public NetOverlayOwnerChangeCmd(BTState appState, DataAccessContext dacx, NetworkOverlayOwnerChange restore) {
    super(appState, dacx);
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
    return ("Net Overlay Owner Change");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() {
    super.undo();
    NetOverlayOwner noo;    
    if (restore_.ownerMode == NetworkOverlay.DYNAMIC_PROXY) {
      noo = dacx_.getGenomeSource().getDynamicProxy(restore_.ownerKey);
    } else {
      noo = dacx_.getGenomeSource().getGenome(restore_.ownerKey);
    }                                                                                 
    noo.overlayChangeUndo(restore_);
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo the operation
  */ 
  
  @Override
  public void redo() {
    super.redo();
    NetOverlayOwner noo;    
    if (restore_.ownerMode == NetworkOverlay.DYNAMIC_PROXY) {
      noo = dacx_.getGenomeSource().getDynamicProxy(restore_.ownerKey);
    } else {
      noo = dacx_.getGenomeSource().getGenome(restore_.ownerKey);
    }       
    noo.overlayChangeRedo(restore_);
    return;
  }
}
