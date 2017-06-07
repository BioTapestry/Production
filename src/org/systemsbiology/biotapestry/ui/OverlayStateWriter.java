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

package org.systemsbiology.biotapestry.ui;

import org.systemsbiology.biotapestry.cmd.undo.SelectionChangeCmd;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** Extends the reader to access write commands
*/

public interface OverlayStateWriter extends OverlayStateOracle {
  
  /***************************************************************************
  **
  ** Set the current network module
  */
  
  public void setCurrentNetModules(TaggedSet keys, boolean forUndo);

  /***************************************************************************
  **
  ** Set the currently revealed modules
  */
  
  public void setRevealedModules(TaggedSet keys, boolean forUndo);
  
  /***************************************************************************
  **
  ** Set the current network overlay key
  */
  
  public void installCurrentSettings(NetModuleFree.CurrentSettings settings); 

  /***************************************************************************
  **
  ** Set the current network overlay key
  */
  
  public SelectionChangeCmd.Bundle setCurrentOverlay(String key, boolean forUndo);
  
}
