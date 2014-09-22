/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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

package org.systemsbiology.biotapestry.db;

import java.util.Iterator;

import org.systemsbiology.biotapestry.ui.Layout;

/****************************************************************************
**
** Interface for layout sources
*/

public interface LayoutSource {
  
  /***************************************************************************
  ** 
  ** Get the given layout.
  */

  public Layout getLayout(String key);

  /***************************************************************************
  ** 
  ** Get the layout key used for the given genome key
  */
  
  public String mapGenomeKeyToLayoutKey(String genomeKey);
  
  /***************************************************************************
  ** 
  ** Get the given layout.
  */

  public Layout getLayoutForGenomeKey(String key);
 
  /***************************************************************************
  **
  ** Get an Iterator over the layouts:
  **
  */
  
  public Iterator<Layout> getLayoutIterator();
  
  /***************************************************************************
  **
  ** Get the layout for the DBGenome:
  **
  */
  
  public Layout getRootLayout();
  
  /***************************************************************************
  **
  ** Start an undo transaction
  */
  
  public DatabaseChange startLayoutUndoTransaction(String key);
 
  /***************************************************************************
  **
  ** Finish an undo transaction
  */
  
  public DatabaseChange finishLayoutUndoTransaction(DatabaseChange change);

  /***************************************************************************
  **
  ** Rollback an undo transaction
  */
  
  public void rollbackLayoutUndoTransaction(DatabaseChange change);

  /***************************************************************************
  ** 
  ** Add the given layout
  */

  public DatabaseChange addLayout(String key, Layout layout);
  
  /***************************************************************************
  ** 
  ** Remove a layout
  */

  public DatabaseChange removeLayout(String key);

}
