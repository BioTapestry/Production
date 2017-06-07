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
import org.systemsbiology.biotapestry.genome.NetModule;
import org.systemsbiology.biotapestry.genome.NetModuleChange;
import org.systemsbiology.biotapestry.genome.NetOverlayOwner;
import org.systemsbiology.biotapestry.genome.NetworkOverlay;
import org.systemsbiology.biotapestry.genome.NetworkOverlayChange;

/****************************************************************************
**
** Handles undos of network overlays
*/

public class NetOverlayChangeCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private NetModuleChange moduleRestore_;
  private NetworkOverlayChange overlayRestore_;
  
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
  
  public NetOverlayChangeCmd(DataAccessContext dacx, NetModuleChange restore) {
    super(dacx);
    moduleRestore_ = restore;
    overlayRestore_ = null;
  }

  /***************************************************************************
  **
  ** Build the command
  */ 
  
  public NetOverlayChangeCmd(DataAccessContext dacx, NetworkOverlayChange restore) {
    super(dacx);
    moduleRestore_ = null;
    overlayRestore_ = restore;
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
    return ("Network Overlay Change");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() {
    super.undo();
    
    if (overlayRestore_ != null) {
      NetOverlayOwner noo;    
      if (overlayRestore_.ownerMode == NetworkOverlay.DYNAMIC_PROXY) {
        noo = dacx_.getGenomeSource().getDynamicProxy(overlayRestore_.noOwnerKey);
      } else {
        noo = dacx_.getGenomeSource().getGenome(overlayRestore_.noOwnerKey);
      }                        
      NetworkOverlay no = noo.getNetworkOverlay(overlayRestore_.overlayKey);    
      no.changeUndo(overlayRestore_);
    } else {
      NetOverlayOwner noo;    
      if (moduleRestore_.ownerMode == NetworkOverlay.DYNAMIC_PROXY) {
        noo = dacx_.getGenomeSource().getDynamicProxy(moduleRestore_.noOwnerKey);
      } else {
        noo = dacx_.getGenomeSource().getGenome(moduleRestore_.noOwnerKey);
      }
      NetworkOverlay no = noo.getNetworkOverlay(moduleRestore_.overlayKey);
      NetModule nm = no.getModule(moduleRestore_.moduleKey);
      nm.changeUndo(moduleRestore_);
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
    if (overlayRestore_ != null) {
      NetOverlayOwner noo;    
      if (overlayRestore_.ownerMode == NetworkOverlay.DYNAMIC_PROXY) {
        noo = dacx_.getGenomeSource().getDynamicProxy(overlayRestore_.noOwnerKey);
      } else {
        noo = dacx_.getGenomeSource().getGenome(overlayRestore_.noOwnerKey);
      }                        
      NetworkOverlay no = noo.getNetworkOverlay(overlayRestore_.overlayKey);    
      no.changeRedo(overlayRestore_);
    } else {
      NetOverlayOwner noo;    
      if (moduleRestore_.ownerMode == NetworkOverlay.DYNAMIC_PROXY) {
        noo = dacx_.getGenomeSource().getDynamicProxy(moduleRestore_.noOwnerKey);
      } else {
        noo = dacx_.getGenomeSource().getGenome(moduleRestore_.noOwnerKey);
      }
      NetworkOverlay no = noo.getNetworkOverlay(moduleRestore_.overlayKey);
      NetModule nm = no.getModule(moduleRestore_.moduleKey);
      nm.changeRedo(moduleRestore_);
    }
    return;
  }
}
