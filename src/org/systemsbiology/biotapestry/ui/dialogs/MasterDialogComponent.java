/*
**    Copyright (C) 2003-2005 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.ui.dialogs;

import org.systemsbiology.biotapestry.util.UndoSupport;


/****************************************************************************
**
** Interface for panels that are shown by the master dialog
*/

public interface MasterDialogComponent {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Refresh the panel
  */
  
  public void refreshData();

  /***************************************************************************
  **
  ** Push the outstanding edits
  */
  
  public void applyPendingPopEdits(UndoSupport support);
  
  
  /***************************************************************************
  **
  ** Push the outstanding edits
  */
  
  public void applyPendingPushEdits(UndoSupport support);  
  
  /***************************************************************************
  **
  ** Ask if the outstanding edits are legit:
  */
  
  public boolean checkAndVetoChanges();  
  
  /***************************************************************************
  **
  ** Get the navigation info
  */
  
  public String getNavigationInfo();
  
}
