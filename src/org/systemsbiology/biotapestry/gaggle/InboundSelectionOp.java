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

package org.systemsbiology.biotapestry.gaggle;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.gaggle.GaggleSupport;
import org.systemsbiology.biotapestry.util.UndoFactory;

/****************************************************************************
**
** Inbound selections
*/

public class InboundSelectionOp extends InboundGaggleOp {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private SelectionSupport.SelectionsForSpecies sfs_;

  /***************************************************************************
  **
  ** Create the op - called on RMI thread
  */

  public InboundSelectionOp(SelectionSupport.SelectionsForSpecies sfs) {
    super();
    sfs_ = sfs;
    return;
  }

  /***************************************************************************
  **
  ** Execute the op - called on AWT thread
  */

  @Override
  public void executeOp(UIComponentSource uics, UndoFactory uFac, StaticDataAccessContext dacx) {
    if (sfs_.selections.isEmpty()) {
      (new GaggleSupport(uics, uFac)).clearFromGaggle(dacx);
    } else {
      (new GaggleSupport(uics, uFac)).selectFromGaggle(sfs_, dacx);
    }
    return;
  }    
}
