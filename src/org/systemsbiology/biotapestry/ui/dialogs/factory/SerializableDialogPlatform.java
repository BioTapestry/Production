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

package org.systemsbiology.biotapestry.ui.dialogs.factory;

import java.util.Map;
import java.util.Set;

import org.systemsbiology.biotapestry.cmd.flow.DialogAndInProcessCmd;
import org.systemsbiology.biotapestry.cmd.flow.FlowMeister.FlowKey;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness.UserInputs;
import org.systemsbiology.biotapestry.ui.xplat.XPlatResponse;
import org.systemsbiology.biotapestry.ui.xplat.dialog.XPlatUIDialog;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** This is a simple dialog platform for the web version
*/

public class SerializableDialogPlatform implements DialogPlatform {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////    
 
  private String servletPath_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SerializableDialogPlatform(String servletPath) {
    servletPath_ = servletPath;
  }  
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHOD
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Answer about our platform
  */  
  
  public Plat getPlatform() {
    return (Plat.WEB);
  }
  
  /***************************************************************************
  **
  ** Get the servlet prefix
  */  
   
  public String getServletPath() {
    return (servletPath_);
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INTERFACE
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public interface Dialog extends ServerControlFlowHarness.Dialog {

 
    /***************************************************************************
    **
    ** Report any errors
    */
     
    public XPlatResponse checkForErrors(UserInputs ui);
    public DialogAndInProcessCmd handleSufResponse(DialogAndInProcessCmd daipc);
    public XPlatUIDialog getDialog();
    public XPlatUIDialog getDialog(FlowKey keyVal);
    

  }

}
