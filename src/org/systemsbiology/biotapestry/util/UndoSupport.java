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

package org.systemsbiology.biotapestry.util;

import java.util.ArrayList;
import javax.swing.undo.CompoundEdit;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.BTUndoCmd;
import org.systemsbiology.biotapestry.cmd.undo.CompoundPostEventCmd2;
import org.systemsbiology.biotapestry.cmd.undo.CompoundPreEventCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.ChangeEvent;

/****************************************************************************
**
** A Class
*/

public class UndoSupport {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE VARIABLES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private CompoundEdit edit_;   
  private CompoundPreEventCmd pre_;
  private CompoundPostEventCmd2 post_;
  private ArrayList<ChangeEvent> preList_;
  private ArrayList<ChangeEvent> postList_;
  private BTState appState_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  ** Keep as-is for version 7, as it is late in the game for a big change. But move to the new one that
  ** provides the DataAccessContext. That will allow operations to occur against different GenomeSources!
  ** @deprecated  Replaced by {@link #UndoSupport(BTState appState, String presentation, DataAccessContext dacx)}
  */

  @Deprecated
  public UndoSupport(BTState appState, String presentation) {
    this(appState, presentation, new DataAccessContext(appState, appState.getGenome()));
  }  

  /***************************************************************************
  **
  ** Constructor.
  **
  */

  public UndoSupport(BTState appState, String presentation, DataAccessContext dacx) {
    appState_ = appState;
    edit_ = new CompoundEdit();
    pre_ = new CompoundPreEventCmd(appState, dacx);
    post_ = new CompoundPostEventCmd2(appState, dacx, appState.getRMan().getString(presentation));
    preList_ = new ArrayList<ChangeEvent>();
    postList_ = new ArrayList<ChangeEvent>();
    edit_.addEdit(pre_);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Rollback the unfinished support session
  */
  
  public void rollback() {
    // FIX ME: Change count is not fixed?
    edit_.end();
    edit_.undo();
    return;
  }  
 
  /***************************************************************************
  **
  ** Add an edit
  */
  
  public void addEdit(BTUndoCmd edit) {
    if (edit.changesModel()) {
      appState_.bumpUndoCount();
    }
    edit_.addEdit(edit);
    return;
  }
  
  /***************************************************************************
  **
  ** Add an edit
  */
  
  public void addEdits(BTUndoCmd[] edits) {
    for (int i = 0; i < edits.length; i++) {
      addEdit(edits[i]);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Add an event to both lists
  */
  
  public void addEvent(ChangeEvent event) {
    if (!preList_.contains(event)) {
      preList_.add(event);
    }
    if (!postList_.contains(event)) {
      postList_.add(event);
    }
    return;
  }

  /***************************************************************************
  **
  ** Add an event to the pre list
  */
  
  public void addPreEvent(ChangeEvent event) {
    if (!preList_.contains(event)) {
      preList_.add(event);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Add an event to the post list
  */
  
  public void addPostEvent(ChangeEvent event) {
    if (!postList_.contains(event)) {
      postList_.add(event);
    }
    return;
  } 
  
  /***************************************************************************
  **
  ** Complete the operation
  */
  
  public void finish() {
    edit_.addEdit(post_);
    pre_.addChangeEvents(preList_);
    post_.addChangeEvents(postList_);
    post_.execute();        
    edit_.end();
    appState_.getUndoManager().addEdit(edit_);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
}
