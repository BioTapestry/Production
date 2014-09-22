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

import java.awt.Component;
import java.util.HashMap;
import javax.swing.JTextField;
import javax.swing.JTable;
import java.util.Map;
import javax.swing.table.TableCellRenderer;

/****************************************************************************
**
** Renderer for ProtoDoubles that disables as needed
*/

public class TrackingDoubleRenderer extends JTextField implements TableCellRenderer {
   
  private HashMap<Integer, TrackingUnit> myTracks_;
  private HandlerAndManagerSource hams_;
  private static final long serialVersionUID = 1L;

  public TrackingDoubleRenderer(Map<Integer, TrackingUnit> toTrack, HandlerAndManagerSource hams) {
    super();
    myTracks_ = new HashMap<Integer, TrackingUnit>(toTrack);
    hams_ = hams;
  }

  public Component getTableCellRendererComponent(JTable table, Object value, 
                                                 boolean isSelected, boolean hasFocus, 
                                                 int row, int column) {
    try {
      TrackingUnit tu = myTracks_.get(new Integer(column));
      setEnabled(tu.isEnabled(row));
      if (value == null) {
        return (this);
      }
      ProtoDouble val = (ProtoDouble)value;
      setText(val.textValue);
    } catch (Exception ex) {
      hams_.getExceptionHandler().displayException(ex);
    }       
    return (this);
  }
}  

