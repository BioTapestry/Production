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


package org.systemsbiology.biotapestry.cmd.flow;

import java.awt.Point;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.ui.dialogs.factory.SerializableDialogPlatform;

/****************************************************************************
**
** Handle Java Web Server Control Flow
*/

public class WebServerControlFlowHarness extends ServerControlFlowHarness {

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
  
  public WebServerControlFlowHarness(BTState appState, SerializableDialogPlatform dPlat) {
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
	  if (cfhui != null) {
		  currDaipc = new DialogAndInProcessCmd(cfhui, currDaipc.currStateX);
		  stepTheFlow();
		  while (currDaipc.state == DialogAndInProcessCmd.Progress.KEEP_PROCESSING) {
			  stepTheFlow();
		  }
	  }
	  return (currDaipc);
  }
  
  /***************************************************************************
  **
  ** Cancel dialog inputs
  */ 
  
  public void userCancel() {
    currDaipc = new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.USER_CANCEL, null, null);
    return;
  }

  /***************************************************************************
  **
  ** Handle user click
  */
  
  @Override
  public DialogAndInProcessCmd handleClick(Point theClick, boolean isShifted, double pixDiam)  {
    currDaipc = currFlow.processClick(theClick, isShifted, pixDiam, currDaipc.currStateX);
    while (currDaipc.state == DialogAndInProcessCmd.Progress.KEEP_PROCESSING) {
      stepTheFlow();
    }
    return (currDaipc);
  }
  
  /***************************************************************************
  ** 
  ** Placeholder when waiting for mouse clicks
  */
  
  public static class PendingMouseClick {
  
    public Point getClickOutput(Map<String, String> vals) {
      String xVal = vals.get("x");
      String yVal = vals.get("y");
      Integer xObj = null;
      Integer yObj = null;
      try {
        xObj = (xVal == null) ? null : Integer.valueOf(xVal);
        yObj = (yVal == null) ? null : Integer.valueOf(yVal);   
      } catch (NumberFormatException nfex) {
        // Just use fact it's still null;
      }
      if ((xObj == null) || (yObj == null)) {
        return (null);
      }   
      return (new Point(xObj.intValue(), yObj.intValue()));
    }
    
    public boolean getShiftOutput(Map<String, String> vals) {
      String shifted = vals.get("shifted");
      Boolean bObj = (shifted == null) ? null : Boolean.parseBoolean(shifted);
      return ((bObj == null) ? false : bObj.booleanValue());
    }
     
    public Set<String> getRequiredParameters() {
      HashSet<String> retval = new HashSet<String>();
      retval.add("x");
      retval.add("y");
      retval.add("shifted");
      return (retval);      
    }
  }
}
