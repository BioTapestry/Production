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


package org.systemsbiology.biotapestry.ui.dialogs.utils;

import java.awt.Dimension;

import javax.swing.JFrame;

import org.systemsbiology.biotapestry.cmd.flow.ClientControlFlowHarness;
import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DesktopDialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;

/****************************************************************************
**
** Abstract base class for stash results dialogs
*/

public abstract class BTTransmitResultsDialog extends BTStashResultsDialog implements DesktopDialogPlatform.Dialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  protected ServerControlFlowHarness.UserInputs request_;
  protected ClientControlFlowHarness cfh_;
  protected DialogPlatform dplat_;
  
  protected JFrame parent_;
  protected boolean doApply_; 
  protected boolean doForStash_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  **
  */ 
  
  protected BTTransmitResultsDialog(ServerControlFlowHarness cfh, String titleResource, Dimension size, int columns, 
                                    ServerControlFlowHarness.UserInputs request, boolean doApply) {
    super(cfh.getUI(), cfh.getDataAccessContext(), titleResource, size, columns);
    doApply_ = doApply;
    request_ = request;
    cfh_ = cfh.getClientHarness();
    dplat_ = cfh.getDialogPlatform();
    parent_ = ((DesktopDialogPlatform)dplat_).getParent();
  }
  
  /***************************************************************************
  **
  ** Constructor for alternate stashing or transmission cases
  **
  */ 
  
  protected BTTransmitResultsDialog(ServerControlFlowHarness cfh, String titleResource, Dimension size, int columns, 
                                    ServerControlFlowHarness.UserInputs request, boolean doApply, boolean doForStash) {
    super(cfh.getUI(), cfh.getDataAccessContext(), titleResource, size, columns);
    doApply_ = doApply;
    doForStash_ = doForStash;
    request_ = request;
    cfh_ = cfh.getClientHarness();
    dplat_ = cfh.getDialogPlatform();
    parent_ = ((DesktopDialogPlatform)dplat_).getParent();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Standard apply
  ** 
  */
 
  @Override
  public void applyAction() {
    if (doForStash_) {
      super.applyAction();
      return;
    }
    if (!doApply_) {
      throw new UnsupportedOperationException();
    } else {
      sendResults(true, true);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
  
  @Override
  public void okAction() {
    if (doForStash_) {
      super.okAction();
      return;
    }  
    if (sendResults(true, false)) {
      setVisible(false);
      dispose();
    }
    return;   
  }
   
  /***************************************************************************
  **
  ** Standard close
  ** 
  */
  
  @Override
  public void closeAction() {
    if (doForStash_) {
      super.closeAction();
      return;
    }     
    if (sendResults(false, false)) {
      setVisible(false);
      dispose();
    }
    return;
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  /***************************************************************************
  **
  ** Send our results to the receiver. 
  ** 
  */
  
  protected boolean sendResults(boolean ok, boolean forApply) {
    request_.clearHaveResults();
    if (ok) {      
      if (bundleForExit(forApply)) {
        cfh_.sendUserInputs(request_);
        return (true);
      } else {
        return (false);
      }
    } else {
      cfh_.sendUserInputs(null);
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Implement this if you want to do stashing
  ** 
  */
  
  protected boolean stashForOK() {
    throw new IllegalStateException();
  }
   
  /***************************************************************************
  **
  ** Do the stashing 
  ** 
  */
  
  protected abstract boolean bundleForExit(boolean forApply);

}
