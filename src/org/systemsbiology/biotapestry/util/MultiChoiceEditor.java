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


package org.systemsbiology.biotapestry.util;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.table.TableCellEditor;
import javax.swing.event.CellEditorListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import javax.swing.event.ChangeEvent;

/***************************************************************************
**
** Class for editing multiple check boxes
*/
  
public class MultiChoiceEditor extends JPanel implements TableCellEditor {
   
  private ArrayList<CellEditorListener> listeners_;
  private JCheckBox[] boxes_;
  protected ChoiceEntrySet templateValue_;  
  protected ChoiceEntrySet origValue_;
  private int currRow_;
  private int currColumn_;
  private HandlerAndManagerSource hams_;
  private static final long serialVersionUID = 1L;
   
  public MultiChoiceEditor(Object valueObject, HandlerAndManagerSource hams) {
    super();
    hams_ = hams;
    valueFill(valueObject);    
    listeners_ = new ArrayList<CellEditorListener>();
    currRow_ = -1;
    currColumn_ = -1;
  }
  
  public void replaceValues(MultiChoiceEditor.ChoiceEntrySet ces) {
    removeAll();
    valueFill(ces);
    return;
  }
    
  private void valueFill(Object valueObject) {
     
    ChoiceEntrySet ces = (ChoiceEntrySet)valueObject;   
    int size = ces.values.length;
    boxes_ = new JCheckBox[size];
     
    setLayout(new GridLayout(1, size));
  
    for (int i = 0; i < size; i++) {
      ChoiceEntry ce = ces.values[i];
      boxes_[i] = new JCheckBox(ce.display, ce.selected);
      add(boxes_[i]);
    }
    
    templateValue_ = new ChoiceEntrySet(ces);
    return;
  }
  
  private void toBoxes(ChoiceEntrySet ces) {     
    int size = ces.values.length;  
    for (int i = 0; i < size; i++) {
      ChoiceEntry ce = ces.values[i];
      boxes_[i].setSelected(ce.selected);
    }    
    return;
  }    
  
  public Component getTableCellEditorComponent(JTable table, Object value, 
                                               boolean isSelected,
                                               int row, int column) {
    try {
      if (value == null) {
        return (this);
      }
      currRow_ = row;
      currColumn_ = column;
      ChoiceEntrySet ces = (ChoiceEntrySet)value;  
      toBoxes(ces);
      table.setRowSelectionInterval(row, row);      
      table.setColumnSelectionInterval(column, column);
      origValue_ = new ChoiceEntrySet(ces);
    } catch (Exception ex) {
      hams_.getExceptionHandler().displayException(ex);
    }
    return (this);             
  }
    
  public int getCurrRow() {
    return (currRow_);
  }
    
  public int getCurrColumn() {
    return (currColumn_);
  }
  
  public boolean entryIsValid() {  
    return ((currRow_ != -1) && (currColumn_ != -1));
  }
        
  public void addCellEditorListener(CellEditorListener l) {
    try {
      this.listeners_.add(l);
    } catch (Exception ex) {
      hams_.getExceptionHandler().displayException(ex);
    }
    return;
  }
    
  public void cancelCellEditing() {
    try {
      editingCanceled();
    } catch (Exception ex) {
      hams_.getExceptionHandler().displayException(ex);
    }
    return;
  }
  
  public ChoiceEntrySet fromBoxes() {    
    ChoiceEntrySet ces = new ChoiceEntrySet(templateValue_);   
    int size = ces.values.length;  
    for (int i = 0; i < size; i++) {
      ChoiceEntry ce = ces.values[i];
      ce.selected = boxes_[i].isSelected();
    }    
    return (ces);
  }  
  
  public Object getCellEditorValue() {
    Object retval = null;
    try {
      retval = fromBoxes();
    } catch (Exception ex) {
      hams_.getExceptionHandler().displayException(ex);
    }
    return (retval);
  }

  public boolean isCellEditable(EventObject anEvent) {
    return (true);
  }
    
  public void removeCellEditorListener(CellEditorListener l) {
    try {
      listeners_.remove(l);
    } catch (Exception ex) {
      hams_.getExceptionHandler().displayException(ex);
    }
    return;
  }
    
  public boolean shouldSelectCell(EventObject anEvent) {
    return (true);
  }

  public boolean stopCellEditing() {
    try {
      editingStopped();
    } catch (Exception ex) {
      hams_.getExceptionHandler().displayException(ex);
    }
    return (true);
  }
  
  public void addItemListener(ItemListener l) {
    int size = boxes_.length;  
    for (int i = 0; i < size; i++) {
      boxes_[i].addItemListener(l);
    }   
    return;
  }
  
  public void removeItemListener(ItemListener l) {
    int size = boxes_.length;  
    for (int i = 0; i < size; i++) {
      boxes_[i].removeItemListener(l);
    }   
    return;
  }  
  
  private void editingCanceled() {
    currRow_ = -1;
    currColumn_ = -1;
    if (origValue_ != null) {
      toBoxes(origValue_);
    }
    ChangeEvent cev = new ChangeEvent(this);
    ArrayList<CellEditorListener> lCopy = new ArrayList<CellEditorListener>(listeners_);
    Iterator<CellEditorListener> lit = lCopy.iterator();
    while (lit.hasNext()) {
      CellEditorListener cel = lit.next();
      cel.editingCanceled(cev);
    }
    return;
  }
    
  private void editingStopped() {
    currRow_ = -1;
    currColumn_ = -1;
    ChangeEvent cev = new ChangeEvent(this);
    ArrayList<CellEditorListener> lCopy = new ArrayList<CellEditorListener>(listeners_);      
    Iterator<CellEditorListener> lit = lCopy.iterator();
    while (lit.hasNext()) {
      CellEditorListener cel = lit.next();
      cel.editingStopped(cev);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Used for each choice entry
  */
  
  public static class ChoiceEntry {
    public boolean selected;
    public String display;
    public String internal;
    
    public ChoiceEntry(String display, String internal, boolean selected) {
      this.display = display;
      this.internal = internal;
      this.selected = selected;      
    }
    
    public ChoiceEntry(ChoiceEntry other) {
      this.display = other.display;
      this.internal = other.internal;
      this.selected = other.selected;      
    }
    
    public boolean equals(Object other) {
      if (other == this) {
        return (true);
      }
      if (!(other instanceof ChoiceEntry)) {
        return (false);
      }
      ChoiceEntry otherCE = (ChoiceEntry)other;
      return ((otherCE.selected == this.selected) &&
              (otherCE.display.equals(this.display)) &&
              (otherCE.internal.equals(this.internal)));
    }
    
    public String toString() {
      return (display + " (" + internal + ") : " + selected); 
    }

  }   
  
  /***************************************************************************
  **
  ** Used for the set of entries
  */
  
  public static class ChoiceEntrySet {
    public ChoiceEntry[] values;

    public ChoiceEntrySet() {
      values = new ChoiceEntry[0]; 
    } 
    
    public ChoiceEntrySet(ChoiceEntry[] values) {
      int size = values.length;
      this.values = new ChoiceEntry[size];
      for (int i = 0; i < size; i++) {
        this.values[i] = new ChoiceEntry(values[i]);
      } 
    }
    
    public ChoiceEntrySet(ChoiceEntrySet other) {
      this(other.values); 
    }
    
    public boolean equals(Object other) {
      if (other == this) {
        return (true);
      }
      if (!(other instanceof ChoiceEntrySet)) {
        return (false);
      }
      ChoiceEntrySet otherCES = (ChoiceEntrySet)other;
      if (otherCES.values.length != this.values.length) {
        return (false);
      }
      int size = this.values.length;
      for (int i = 0; i < size; i++) {
        if (!this.values[i].equals(otherCES.values[i])) {
          return (false);
        }
      }
      return (true);
    }
    
    public String toString() {
      StringBuffer buf = new StringBuffer();
      int size = values.length;
      for (int i = 0; i < size; i++) {
        buf.append(values[i]);
        buf.append("\n");
      }
      return (buf.toString()); 
    }
  }
}
  
