/*
**    Copyright (C) 2003-2013 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.cmd.flow;

import java.awt.Point;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;

/****************************************************************************
**
** Handle Batch Job Control Flow
*/

public class BatchJobControlFlowHarness extends ServerControlFlowHarness {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public BatchJobControlFlowHarness(BTState appState, SerializableDialogPlatform dPlat) {
    super(appState, dPlat);
    currFlow = null;
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHOD
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the client side harness
  */ 
  
  @Override
  public ClientControlFlowHarness getClientHarness() {
    return (null);
  }
  
  /***************************************************************************
  **
  ** Handle dialog inputs
  */ 
  
  @Override
  public DialogAndInProcessCmd receiveUserInputs(UserInputs cfhui) {
    throw new IllegalStateException();
  }
  
  /***************************************************************************
  **
  ** Handle user click
  */
  
  @Override
  public DialogAndInProcessCmd handleClick(Point theClick, boolean isShifted, double pixDiam)  {
    throw new IllegalStateException();
  }
}
