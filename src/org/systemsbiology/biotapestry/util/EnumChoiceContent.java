/*
**    Copyright (C) 2003-2015 Institute for Systems Biology 
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;

/****************************************************************************
**
** Utility for choice menus
*/

public class EnumChoiceContent<V extends Comparable<V>> implements Comparable<EnumChoiceContent<V>> {
  
  public String name;
  public V val;
  public int index;
  
  public EnumChoiceContent(String name, V val, int index) {
    this.name = name;
    this.val = val;
    this.index = index;
  }
  
  public EnumChoiceContent(String name, V val) {
    this.name = name;
    this.val = val;
    this.index = -1;
  }
  
   public EnumChoiceContent(EnumChoiceContent<V> other) {
    this.name = other.name;
    this.val = other.val;
    this.index = other.index;
  } 
  
  public String toString() {
    return (name);
  }
  
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return (true);
    }
    if (!(other instanceof EnumChoiceContent<?>)) {
      return (false);
    }
    EnumChoiceContent<?> occ = (EnumChoiceContent<?>)other;
    
    if (this.val == null) {
      if (occ.val != null) {
        return (false);
      }
    } else if (!this.val.equals(occ.val)) {
      return (false);
    }
    
    if (name == null) {
      return (occ.name == null);
    }

    if (!name.equals(occ.name)) {
      return (false);
    }
    
    return (this.index == occ.index);
  }
  
  public int hashCode() {
    return ((val == null) ? 0 : val.hashCode());
  }
  
  public int compareTo(EnumChoiceContent<V> otherOCC) {

    String useValThis = (this.name == null) ? "" : this.name;
    String useValOther = (otherOCC.name == null) ? "" : otherOCC.name;  
    
    int compName = useValThis.compareToIgnoreCase(useValOther);
    if (compName != 0) {
      return (compName);
    }
    compName = useValThis.compareTo(useValOther);
    if (compName != 0) {
      return (compName);
    }
    
    if (this.val == null) {
      return ((otherOCC.val == null) ? 0 : 1);
    }
    
    if (otherOCC.val == null) {
      return (1);
    }
    
    int compInt = this.val.compareTo(otherOCC.val);
    if (compInt != 0) {
      return (compInt);
    }
    
    return (this.index - otherOCC.index);
  } 
  
  /***************************************************************************
  **
  ** Used for editable combo boxes
  */
  
  public static class EditableComboChoiceRenderer extends JComboBox implements TableCellRenderer {
   
    private static final long serialVersionUID = 1L;
    protected HandlerAndManagerSource hams_;
    
    public EditableComboChoiceRenderer(List values, HandlerAndManagerSource hams) {
      super();
      hams_ = hams;
      for (int i = 0; i < values.size(); i++) {
        this.addItem(((EnumChoiceContent)(values.get(i))).name);
      }
      this.setEditable(true);      
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value, 
                                                   boolean isSelected, boolean hasFocus, 
                                                   int row, int column) {
      try {
        JTextComponent jtc = (JTextComponent)(getEditor().getEditorComponent());
        jtc.setBackground((isSelected) ? table.getSelectionBackground() : table.getBackground());
        if (value == null) {
          return (this);
        }
        setSelectedItem(((EnumChoiceContent)value).name);
      } catch (Exception ex) {
        hams_.getExceptionHandler().displayException(ex);
      }      
      return (this);             
    }
  }  
 
  /***************************************************************************
  **
  ** Used for noneditable combo boxes
  */
  
  public static class ReadOnlyComboChoiceRenderer extends JComboBox implements TableCellRenderer {
   
    private static final long serialVersionUID = 1L;
    protected HandlerAndManagerSource hams_;
    
    public ReadOnlyComboChoiceRenderer(List values, HandlerAndManagerSource hams) {
      super();
      hams_ = hams;
      for (int i = 0; i < values.size(); i++) {
        this.addItem(values.get(i));
      }
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value, 
                                                   boolean isSelected, boolean hasFocus, 
                                                   int row, int column) {
      try {                                               
        if (value == null) {
          return (this);
        }    
        setSelectedItem(value);
      } catch (Exception ex) {
        hams_.getExceptionHandler().displayException(ex);
      }      
      return (this);             
    }
  }
  
  /***************************************************************************
  **
  ** Used for noneditable combo boxes that are enabled/disabled by remote state
  */
  
  public static class TrackingReadOnlyComboChoiceRenderer extends ReadOnlyComboChoiceRenderer {
   
    private static final long serialVersionUID = 1L;
    private HashMap myTracks_;

    public TrackingReadOnlyComboChoiceRenderer(List values, Map toTrack, HandlerAndManagerSource hams) {
      super(values, hams);
      myTracks_ = new HashMap(toTrack);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, 
                                                   boolean isSelected, boolean hasFocus, 
                                                   int row, int column) {
      try { 
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TrackingUnit tu = (TrackingUnit)myTracks_.get(new Integer(column));
        setEnabled(tu.isEnabled(row));
      } catch (Exception ex) {
        hams_.getExceptionHandler().displayException(ex);
      }      
      return (this);             
    }
  }
    
  /***************************************************************************
  **
  ** Used for editable cells
  */
  
  public static class EditableComboChoiceEditor extends EditableComboBoxEditor {

    private static final long serialVersionUID = 1L;
    private HashMap<String, EnumChoiceContent<?>> backing_;
    
    public EditableComboChoiceEditor(Object valueObject, HandlerAndManagerSource hams) {
      super(valueObject, hams);
    }
    
    public void valueFill(Object valueObject) {
      List<EnumChoiceContent<?>> list = (List<EnumChoiceContent<?>>)valueObject;
      backing_ = new HashMap<String, EnumChoiceContent<?>>();
      for (int i = 0; i < list.size(); i++) {
        String display = list.get(i).name; 
        this.addItem(display);
        backing_.put(display, list.get(i));
      }
      return;
    }
    
    public Object displayStringToValue(String display) {
      EnumChoiceContent<?> region = backing_.get(display);
      if (region == null) {  // new value
        throw new IllegalArgumentException();
       // return (new EnumChoiceContent(display, display, -1, -1));
      }
      return (new EnumChoiceContent(backing_.get(display)));
    }
    
    public String valueToDisplayString(Object value) {
      return (((EnumChoiceContent)value).name);
    }
  }  
  
  /***************************************************************************
  **
  ** Used for read-only editor
  */
  
  public static class ReadOnlyComboChoiceEditor extends ComboBoxEditor {
    
    private List<EnumChoiceContent<?>> backing_;
    private static final long serialVersionUID = 1L;
    
    public ReadOnlyComboChoiceEditor(List values, HandlerAndManagerSource hams) {
      super(values, hams);
    }
    
    public Object indexToValue(int index) {
      return (new EnumChoiceContent(backing_.get(index)));
    }
    
    public void valueFill(Object valueObject) {
      List<EnumChoiceContent<?>> list = (List<EnumChoiceContent<?>>)valueObject;
      backing_ = new ArrayList<EnumChoiceContent<?>>();
      for (int i = 0; i < list.size(); i++) {
        this.addItem(list.get(i));
        backing_.add(list.get(i));
      }
    }
    
    public int valueToIndex(Object value) {
      return (((EnumChoiceContent)value).index);
    }
    
    public void reviseValue(Object value) {
      origValue_ = value;
      setSelectedIndex(((EnumChoiceContent)value).index);
      return;
    }     
  }
}
