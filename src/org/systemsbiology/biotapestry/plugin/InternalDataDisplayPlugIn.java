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

package org.systemsbiology.biotapestry.plugin;

/****************************************************************************
**
** Interface for plugins that display experimental data using internal node IDs.
** This interface is being kept around for legacy reasons only!  Should now 
** be using InternalNodeDataDisplayPlugIn for new development!
*/

public interface InternalDataDisplayPlugIn extends DataDisplayPlugIn {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public static final boolean IS_INTERNAL_ANSWER = true;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Get a string of valid HTML (no html or body tags, please!) to display
  ** as experimental data for the desired item.  Used for internal plug-ins with
  ** more required arguments.
  */
  
  public String getDataAsHTML(String geneId, String name, boolean bigScreen);

}
