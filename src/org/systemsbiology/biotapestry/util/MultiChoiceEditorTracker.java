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

package org.systemsbiology.biotapestry.util;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.table.AbstractTableModel;

/****************************************************************************
**
** Helper to track multiChoice editor
*/

public class MultiChoiceEditorTracker implements ItemListener {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private MultiChoiceEditor multiEdit_;
  private AbstractTableModel table_;
  private HandlerAndManagerSource hams_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

   /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public MultiChoiceEditorTracker(MultiChoiceEditor multiEdit, AbstractTableModel table, HandlerAndManagerSource hams) { 
    multiEdit_ = multiEdit;
    table_ = table;
    hams_ = hams;

    multiEdit.addItemListener(this);
  }

  public void updateListenerStatus() {
    multiEdit_.removeItemListener(this);
    multiEdit_.addItemListener(this);
    return;
  }  

  public void itemStateChanged(ItemEvent e) {
    try {
      if (multiEdit_.entryIsValid()) { 
        int cc = multiEdit_.getCurrColumn();
        int cr = multiEdit_.getCurrRow();
        table_.setValueAt(multiEdit_.fromBoxes(), cr, cc);
      }
    } catch (Exception ex) {
      hams_.getExceptionHandler().displayException(ex);
    }
    return;
  }
}
