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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/****************************************************************************
**
** Fixed Size Button
*/

public class JTabbedPaneWithPopup extends JTabbedPane {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private ArrayList<MouseListenerWrapper> myList_;
  private JPopupMenu myPopup_;
  private ArrayList<InformedAction> myActions_;
  private HandlerAndManagerSource hams_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Constructor for the tabbed pane with a popup
  */

  public JTabbedPaneWithPopup(HandlerAndManagerSource hams) {
    super();
    hams_ = hams;
    myActions_ = new ArrayList<InformedAction>();
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Add the actions
  */

  public void setActions(List<InformedAction> aacts) {
    init();
    for (InformedAction aa : aacts) {
      myPopup_.add(aa);
      myActions_.add(aa);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Constructor
  */

  @Override
  public void addMouseListener(MouseListener l) {
    init();
    MouseListenerWrapper nml = new MouseListenerWrapper(l);
    myList_.add(nml);
    super.addMouseListener(nml);
    return;
  }  
  
   
  /***************************************************************************
  **
  ** Make pictures square
  */
  
  @Override  
  public void removeMouseListener(MouseListener l) {
    if (myList_ == null) {
      super.removeMouseListener(l);
    } else {
      for (int i = 0; i < myList_.size(); i++) {
        MouseListenerWrapper mlw = myList_.get(i);
        if (mlw.wrapsRealGuy(l)) {
          super.removeMouseListener(mlw);
          myList_.remove(mlw);
          return;
        }
      }
    }
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Constructor
  */
  
  private void init() {
    if (myList_ == null) {
      myList_ = new ArrayList<MouseListenerWrapper>();
      myPopup_ = new JPopupMenu();
      myPopup_.addPopupMenuListener(new PopupHandler());
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public class MouseListenerWrapper implements MouseListener {

    private MouseListener realGuy_;

    public MouseListenerWrapper(MouseListener realGuy) {
      realGuy_ = realGuy;
    }

    public void mouseClicked(MouseEvent me) {
      if (me.isPopupTrigger() || SwingUtilities.isRightMouseButton(me)) {
        triggerPopup(me.getPoint());
        return;
      }
      realGuy_.mouseClicked(me);
      return;
    }

    public void mousePressed(MouseEvent me) {
      if (me.isPopupTrigger() || SwingUtilities.isRightMouseButton(me)) {
        triggerPopup(me.getPoint());
        return;
      }
      realGuy_.mousePressed(me);
      return;
    }

    public void mouseReleased(MouseEvent me) {
      if (me.isPopupTrigger() || SwingUtilities.isRightMouseButton(me)) {
        triggerPopup(me.getPoint());
        return;
      }
      realGuy_.mouseReleased(me);
      return;
    }

    public void mouseEntered(MouseEvent me) {
      realGuy_.mouseEntered(me);
      return;
    }

    public void mouseExited(MouseEvent me) {
      realGuy_.mouseExited(me);
      return;
    }
    
    public boolean wrapsRealGuy(MouseListener ml) {
      return (realGuy_ == ml); 
    }
    
    public MouseListener getRealGuy() {
      return (realGuy_); 
    }
     
    private void triggerPopup(Point pt) {
      try {
        int count = JTabbedPaneWithPopup.this.getTabCount();
        for (int i = 0; i < count; i++) {
          Rectangle rect = JTabbedPaneWithPopup.this.getBoundsAt(i);
          if (rect.contains(pt)) {
            myPopup_.show(JTabbedPaneWithPopup.this, pt.x, pt.y);
            for (InformedAction aa : myActions_) {
              aa.selectedTab(i);
            }
            return;
          }
        }
        return;
      } catch (Exception ex) {
        hams_.getExceptionHandler().displayException(ex);
      }     
    }  
  }
  
  /***************************************************************************
  **
  ** Listens for popup events
  */  
  
  public static class PopupHandler implements PopupMenuListener {
    
    public void popupMenuCanceled(PopupMenuEvent e) {
    }
    
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }
    
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    }
  } 
  
  /***************************************************************************
  **
  ** Adds tab info to a basic action
  */  
  
  public static abstract class InformedAction extends AbstractAction {
    
    protected int selectedTab_;
    
    public void selectedTab(int tabNum) {
      selectedTab_ = tabNum;
      return;
    }
  }   
}
