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

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.Action;
 
/***************************************************************************
** 
** Useful for generalizing menu building tasks
*/

public class MenuItemTarget {  
   
  private JPopupMenu popup_;
  private JMenu menu_;

  public MenuItemTarget(JPopupMenu popup) {
    popup_ = popup;
  }

  public MenuItemTarget(JMenu menu) {
    menu_ = menu;
  }

  public void removeAll() {
    if (popup_ != null) {
      popup_.removeAll();
    } else {
      menu_.removeAll();
    }
    return;
  }
  
  public JComponent add(JSeparator sep) {
    JComponent retval;
    if (popup_ != null) {
      retval = (JComponent)popup_.add(sep);
    } else {
      retval = (JComponent)menu_.add(sep);
    }
    return (retval);
  }

  public JComponent add(JMenuItem item) {
    JComponent retval;
    if (popup_ != null) {
      retval = (JComponent)popup_.add(item);
    } else {
      retval = (JComponent)menu_.add(item);
    }
    return (retval);
  }

  public JComponent add(Action act) {
    JComponent retval;
    if (popup_ != null) {
      retval = (JComponent)popup_.add(act);
    } else {
      retval = (JComponent)menu_.add(act);
    }
    return (retval);
  } 
  
  public int getComponentCount() {
    if (popup_ != null) {
      return (popup_.getComponentCount());
    } else {
      return (menu_.getComponentCount());
    }
  } 
 
  public void add(JComponent jcp) {
    if (popup_ != null) {
      popup_.add(jcp);
    } else {
      menu_.add(jcp);
    }
    return;
  }   
}
