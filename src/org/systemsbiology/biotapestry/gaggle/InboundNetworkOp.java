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

package org.systemsbiology.biotapestry.gaggle;

import java.util.ArrayList;
import java.util.List;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.io.LoadSaveSupport;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstruction;
import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** Class for operation to build a network
*/

public class InboundNetworkOp extends InboundGaggleOp {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<BuildInstruction> instruct_;
  private String species_;
   
  /***************************************************************************
  **
  ** Create the op - called on RMI thread
  */

  public InboundNetworkOp(BTState appState, String species, List<BuildInstruction> instruct) {
    super(appState);
    instruct_ = new ArrayList<BuildInstruction>();
    int iLen = instruct.size();
    for (int i = 0; i < iLen; i++) {
      instruct_.add(instruct.get(i).clone());
    }
    species_ = species;
    return;
  }

////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Execute the op - called on AWT thread
  */

  public void executeOp() {
    LoadSaveSupport.NetworkBuilder nb = appState_.getLSSupport().getNetworkBuilder();
    nb.doNetworkBuild(species_, instruct_, new DataAccessContext(appState_, appState_.getGenome()));
    return;
  } 
}
