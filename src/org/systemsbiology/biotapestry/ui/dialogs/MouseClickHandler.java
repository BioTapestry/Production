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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.systemsbiology.biotapestry.app.BTState;

/***************************************************************************
** 
** Handles mouse click ugliness
*/

public class MouseClickHandler extends MouseAdapter {

  private final static int CLICK_SLOP_  = 2;
  private final static int DRAG_SLOP_   = 2;

  private Point lastPress_;
  private MouseClickHandlerTarget target_; 
  private BTState appState_;

  public MouseClickHandler(BTState appState, MouseClickHandlerTarget target) {
    appState_ = appState;
    target_ = target;
    lastPress_ = null;
  }
  
  public void mousePressed(MouseEvent me) {
    try {
      lastPress_ = new Point(me.getX(), me.getY());
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }

  public void mouseClicked(MouseEvent me) {
    return;
  }    

  public void mouseReleased(MouseEvent me) {
    try {
      int currX = me.getX();
      int currY = me.getY();
      if (lastPress_ == null) {
        return;
      }
      int lastX = lastPress_.x;
      int lastY = lastPress_.y;
      int diffX = Math.abs(currX - lastX);
      int diffY = Math.abs(currY - lastY);      
      if ((diffX <= CLICK_SLOP_) && (diffY <= CLICK_SLOP_)) {
        lastPress_ = null;
        target_.clickResult(lastX, lastY);
      } else if ((diffX >= DRAG_SLOP_) || (diffY >= DRAG_SLOP_)) {
        lastPress_ = null;
        target_.dragResult(lastX, lastY);
      }
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }
}
