/*
**    Copyright (C) 2003-2011 Institute for Systems Biology 
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


package org.systemsbiology.biotapestry.embedded;

import java.io.InputStream;
import java.util.Set;

/****************************************************************************
**
** Interface for an Embedded BioTapestry Viewer
*/

public interface EmbeddedBioTapestryViewer {
                                                                                                                       
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Call to load a BioTapestry .btp file from the given input stream:
  */ 
  
  public void loadBtp(InputStream is) throws EmbeddedException;

  /***************************************************************************
  **
  ** Call to add a selection change listener.  Sorry, can only add at the
  ** moment, not remove:
  */ 
  
  public void addSelectionChangeListener(ExternalSelectionChangeListener escl);
  
  /***************************************************************************
  **
  ** Get the inventory of all elements
  */ 
  
  public EmbeddedViewerInventory getElementInventory();

  /***************************************************************************
  **
  ** Go to the given model and select the given nodes and links.  If the
  ** operation is not successful, return false.
  */
  
  public boolean goToModelAndSelect(String modelID, Set nodeIDs, Set linkIDs);

  /***************************************************************************
  **
  ** Call when the application is exiting
  */ 
  
  public void callForShutdown();
  
}
