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

package org.systemsbiology.biotapestry.ui;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.systemsbiology.biotapestry.app.BTState;

import java.net.URL;

/***************************************************************************
** 
** This class manages cursors
*/

public class CursorManager  {
  
  private Cursor currCursor_;
  private int errCount_;
  private JPanel pan_;
  private Cursor xCur_;
  private Cursor errCur_;
  private BTState appState_;
  private boolean active_;

  public CursorManager(BTState appState) {
    appState_ = appState.setCursorManager(this);
    errCount_ = 0;
    active_ = !appState_.isHeadless();
    if (active_) {
      pan_ = appState_.getSUPanel().getPanel();
      pan_.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      buildCursors();
    }
  }

  public synchronized void showDefaultCursor() {
    if (active_) {
      currCursor_ = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
      if (errCount_ == 0) {
        pan_.setCursor(currCursor_);
      }
    }
    return;
  }

  public synchronized void showModeCursor() {
    if (active_) {
      currCursor_ = xCur_;
      if (errCount_ == 0) {
        pan_.setCursor(currCursor_);
      }
    }
    return;
  }    

  public synchronized void signalError() {
    if (active_) {
      if (errCount_++ == 0) { 
        pan_.setCursor(errCur_);
      }

      final Timer timer = new Timer(1000, null);
      timer.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          clearError();
          timer.stop();
        }
      });
      timer.setRepeats(false);
      timer.start();
    }
    return;
  }      

  public synchronized void clearError() {
    if (active_) {
      if (--errCount_ == 0) { 
        pan_.setCursor(currCursor_);
      } 
    }
    return;
  } 

  private void buildCursors() {
    if (active_) {
      if (xCur_ == null) {
        Toolkit tkit = Toolkit.getDefaultToolkit();
        URL xCurGif = getClass().getResource("/org/systemsbiology/biotapestry/images/CrossCursor32.gif");
        Point hot = new Point(15, 15);
        Image img = tkit.createImage(xCurGif);
        xCur_ = tkit.createCustomCursor(img, hot, "btCustomCross");
      }
      if (errCur_ == null) {
        Toolkit tkit = Toolkit.getDefaultToolkit();
        URL errCurGif = getClass().getResource("/org/systemsbiology/biotapestry/images/ErrorCursor32.gif");
        Point hot = new Point(15, 15);
        Image img = tkit.createImage(errCurGif);
        errCur_ = tkit.createCustomCursor(img, hot, "btCustomError");
      }
    }
    return;
  }       
}  
