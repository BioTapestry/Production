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

package org.systemsbiology.biotapestry.ui.layouts;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.util.AsynchExitRequestException;
import org.systemsbiology.biotapestry.util.BTProgressMonitor;

/****************************************************************************
**
** Specialty Layout
*/

public interface SpecialtyLayout {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  public static final int TOPOLOGY_HANDLED          = 0;  
  public static final int TOPOLOGY_NOT_HANDLED_STOP = 1;  
  public static final int TOPOLOGY_NOT_HANDLED_OK   = 2;  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the menu name tag for the layout
  */
  
  public String getMenuNameTag();  
    
  
  /***************************************************************************
  **
  ** Get the mnemonic char tag for the layout
  */
  
  public String getMenuMnemonicTag();  
  
  /***************************************************************************
  **
  ** Get the undo string for the layout
  */
  
  public String getUndoString();    
  
  /***************************************************************************
  **
  ** Answer if the given selection is valid
  */
  
  public boolean selectionIsValid(Genome genome, String selected);
  
  /***************************************************************************
  **
  ** Answer if the network topology can be handled by this layout
  */
  
  public int topologyIsHandled(StaticDataAccessContext rcx);  
  
  /***************************************************************************
  **
  ** Answer if the setup works OK.  Error message if a problem, else null if OK
  */
  
  public String setUpIsOK();
  
  /***************************************************************************
  **
  ** Prep the layout
  */
  
  public SpecialtyLayout forkForSubset(SpecialtyLayoutData sld);
  
  /***************************************************************************
  **
  ** Figure out the node positions.  Returns list of created GASCs
  */
  
  public void layoutNodes(BTProgressMonitor monitor) throws AsynchExitRequestException;
  
  /***************************************************************************
  **
  ** Figure out the link routes
  */
  
  public void routeLinks(SpecialtyLayoutEngine.GlobalSLEState gsles, BTProgressMonitor monitor) throws AsynchExitRequestException;
  
  /***************************************************************************
  **
  ** Assign colors
  */
  
  public void assignColors(BTProgressMonitor monitor) throws AsynchExitRequestException;
  
  /***************************************************************************
  **
  ** Get the parameter dialog
  */
  
  public SpecialtyLayoutEngineParamDialogFactory.BuildArgs getParameterDialogBuildArgs(UIComponentSource uics, Genome genome, String selectedID, boolean forSubset);  
  
}
