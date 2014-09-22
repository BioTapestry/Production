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
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;

/****************************************************************************
**
** Harness for running a command
*/

public abstract class ServerControlFlowHarness {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public enum ClickResult {SELECTED, CANCELLED, UNSELECTED, ERROR, PRESENT, ACCEPT_DELAYED, PROCESSED, REJECT, ACCEPT};
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  protected DialogPlatform dPlat;
  protected ControlFlow currFlow;
  protected DialogAndInProcessCmd currDaipc;
  protected BTState appState;
  protected DataAccessContext dacx;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public ServerControlFlowHarness(BTState appState, DialogPlatform dPlat) {
    this.appState = appState;
    this.dPlat = dPlat;
    currFlow = null;
    currDaipc = null;
  }
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the client side
  */ 
  
  public abstract ClientControlFlowHarness getClientHarness();
  
  /***************************************************************************
  **
  ** Get dialog platform
  */ 
   
  public DialogPlatform getDialogPlatform() {
    return (dPlat);
  }
  
  /***************************************************************************
  **
  ** Clear current control flow
  */ 
   
  public void clearFlow() {
    currFlow = null;
    currDaipc = null;
  }
  
  /***************************************************************************
  **
  ** Get application state
  */
    
  public BTState getBTState() {
    return (appState);
  }
 
  /***************************************************************************
  **
  ** Get application state
  */ 
    
  public DataAccessContext getDataAccessContext() {
    return (dacx);
  }

  /***************************************************************************
  **
  ** Get current control flow
  */ 
   
  public ControlFlow getCurrFlow() {
    return (currFlow);
  }
  
  /***************************************************************************
  **
  ** Get current DAIPC
  */ 
   
  public DialogAndInProcessCmd getCurrDAIPC() {
    return (currDaipc);
  }
  
  /***************************************************************************
  **
  ** Set current control flow
  */ 
   
  public void initFlow(ControlFlow theFlow, DataAccessContext dacx) {
    currFlow = theFlow;
    currDaipc = null;
    this.dacx = dacx;
  }
 
  /***************************************************************************
  **
  ** Step the flow 
  */ 
  
  public DialogAndInProcessCmd stepTheFlow()  {
    currDaipc = currFlow.processNextStep(this, currDaipc);
    return (currDaipc);
  } 
 
  /***************************************************************************
  **
  ** Step the flow 
  */ 
  
  public DialogAndInProcessCmd stepTheFlow(DialogAndInProcessCmd.CmdState cms)  {
    currDaipc = (cms != null) ? new DialogAndInProcessCmd(DialogAndInProcessCmd.Progress.NORMAL, cms) : null; 
    return (stepTheFlow());
  } 
   
  /***************************************************************************
  **
  ** Handle dialog inputs. Return may be null for some platforms (e.g. desktop)
  */ 
  
  public abstract DialogAndInProcessCmd receiveUserInputs(UserInputs cfhui);
 
  /***************************************************************************
  **
  ** Handle user click.  Extract click result from the return
  */ 
  
  public abstract DialogAndInProcessCmd handleClick(Point theClick, boolean isShifted, double pixDiam);
 
  /***************************************************************************
  **
  ** Process an out-of-flow question/response
  */ 
     
  public RemoteRequest.Result receiveRemoteRequest(RemoteRequest qubom) {
    return (currFlow.processRemoteRequest(qubom, currDaipc.currStateX));   
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  public interface Dialog {
    public boolean isModal();
  }
   
  public interface UserInputs {
    public void clearHaveResults();
    public boolean haveResults();
    public boolean isForApply();
  } 
}
