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

package org.systemsbiology.biotapestry.ui;

import java.awt.Rectangle;
import java.util.Map;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** Interface for zoom presentation.
**
** NOTE: The DataAccessContexts passed through this interface are both DynamicDataAccessContexts and
** StaticDataAccessContexts! Thus the base class appears in the signature!
**
*/

public interface ZoomPresentation {
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Return the required size of the layout
  */
  
  public Rectangle getRequiredSize(DataAccessContext rcx, 
                                   boolean doComplete, boolean useBuffer, 
                                   boolean doModules, boolean doModuleLinks, 
                                   String ovrKey, TaggedSet modSet, Map<String, Layout.OverlayKeySet> allKeys);
 
  /***************************************************************************
  **
  ** Return the required size of the selected items.  Null if nothing is
  ** selected.
  */
  
  public Rectangle getSelectionSize(DataAccessContext rcx); 

  /***************************************************************************
  **
  ** Return the required size of the current zoom selection item.  Null if nothing is
  ** selected.
  */
  
  public Rectangle getCurrentSelectionSize(DataAccessContext rcx);   
 
  /***************************************************************************
  **
  ** Answer if we have a current selection
  */
  
  public boolean haveCurrentSelection();
  
  /***************************************************************************
  **
  ** Answers if we have mutliple selections (i.e. can cycle through selections) 
  */
  
  public boolean haveMultipleSelections();
 
  /***************************************************************************
  **
  ** Bump to the next selection for zoom
  */
  
  public void bumpNextSelection(DataAccessContext rcx);
  
  /***************************************************************************
  **
  ** Bump to the previous selection for zoom
  */
  
  public void bumpPreviousSelection(DataAccessContext rcx);
  
  /***************************************************************************
  **
  ** Set the zoom
  */
  
  public void setPresentationZoomFactor(double zoom, DataAccessContext rcx);
    
}
