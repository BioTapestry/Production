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
import org.systemsbiology.biotapestry.ui.Layout;

/****************************************************************************
**
** Handles the bus move operation
*/

public class PropChangeCmd extends BTUndoCmd {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private Layout.PropChange[] restore_;
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
  
  public PropChangeCmd(BTState appState, DataAccessContext dacx, Layout.PropChange[] restore) {
    super(appState, dacx);
    restore_ = restore;
    for (int i = 0; i < restore.length; i++) {
      Layout.PropChange pc = restore[i];
      // Failed moves may return nulls among the successes:
      if (pc == null) {
        continue;
      }
      if ((pc.newProps != null) && (pc.linkIDs == null)) {
        throw new IllegalArgumentException();
      }
    }
  }
  
  /***************************************************************************
  **
  ** Build the command
  */ 
  
  public PropChangeCmd(BTState appState, DataAccessContext dacx, Layout.PropChange restore) {
    super(appState, dacx);
    restore_ = new Layout.PropChange[1];
    restore_[0] = restore;
    for (int i = 0; i < restore_.length; i++) {
      Layout.PropChange pc = restore_[i];
      // Failed moves may return nulls among the successes:
      if (pc == null) {
        continue;
      }
      if ((pc.newProps != null) && (pc.linkIDs == null)) {
        throw new IllegalArgumentException();
      }
    }
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
    return ("Property Change");
  }

  /***************************************************************************
  **
  ** Undo the operation
  */ 
  
  @Override
  public void undo() {
    super.undo();
    
    // Gotta do these in reverse order to undo correctly
    for (int i = restore_.length - 1; i >= 0; i--) {
      Layout.PropChange pc = restore_[i];
      // Failed moves may return nulls among the successes:
      if (pc == null) {
        continue;
      }
      Layout layout = dacx_.lSrc.getLayout(pc.layoutKey);
      if ((pc.orig != null) || (pc.newProps != null)) {
        layout.linkChangeUndo(pc);
      } else if ((pc.nOrig != null) || (pc.nNewProps != null)) {
        layout.nodeChangeUndo(pc);
      } else if ((pc.ntOrig != null) || (pc.ntNewProps != null)) {
        layout.noteChangeUndo(pc); 
      } else if ((pc.grOrig != null) || (pc.grNewProps != null)) {
        layout.groupChangeUndo(pc); 
      } else if (pc.dLocKey != null) {
        layout.dataPosChangeUndo(pc);
      } else if (pc.metaOrig != null) {
        layout.metaChangeUndo(pc);
      } else if ((pc.nopOrig != null) || (pc.nopNew != null)) {
        layout.overlayChangeUndo(pc);
      } else if ((pc.nmpOrig != null) || (pc.nmpNew != null)) {
        layout.netModChangeUndo(pc);
      } else if ((pc.nmlpOrig != null) || (pc.nmlpNew != null) || (pc.nmlpTieLinkIDOrig != null)) {
        layout.netModLinkChangeUndo(pc);
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
    for (int i = 0; i < restore_.length; i++) {
      Layout.PropChange pc = restore_[i];
      if (pc == null) {
        continue;
      }
      Layout layout = dacx_.lSrc.getLayout(pc.layoutKey);
      if ((pc.newProps != null) || (pc.orig != null)) {
        layout.linkChangeRedo(pc);
      } else if ((pc.nNewProps != null) || (pc.nOrig != null)) {
        layout.nodeChangeRedo(pc);
      } else if ((pc.ntNewProps != null)|| (pc.ntOrig != null)) {
        layout.noteChangeRedo(pc);
      } else if ((pc.grNewProps != null) || (pc.grOrig != null)) {
        layout.groupChangeRedo(pc);
      } else if (pc.dLocKey != null) {
        layout.dataPosChangeRedo(pc);
      } else if (pc.newMeta != null) {
        layout.metaChangeRedo(pc);
      } else if ((pc.nopOrig != null) || (pc.nopNew != null)) {
        layout.overlayChangeRedo(pc);
      } else if ((pc.nmpOrig != null) || (pc.nmpNew != null)) {
        layout.netModChangeRedo(pc);
      } else if ((pc.nmlpOrig != null) || (pc.nmlpNew != null) || (pc.nmlpTieLinkIDOrig != null)) {
        layout.netModLinkChangeRedo(pc);
      }
    }
    return;
  }
}
