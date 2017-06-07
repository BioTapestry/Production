/*
**    Copyright (C) 2003-2017 Institute for Systems Biology 
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.FontManager;

/****************************************************************************
**
** An animated split pane
*/

public class AnimatedSplitPane extends JPanel implements ActionListener {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private static final double USE_INC_ = 0.075;
  
  private double currSplit_;
  private double inc_;
  private boolean doFollowOn_;
  private Timer zoomTimer_;
  private AnimatedSplitListener asl_; 
  private AnimatedSplitPaneLayoutManager.PanelForSplit myTop_;
  private AnimatedSplitPaneLayoutManager.PanelForSplit myBottom_;
  private JPanel barPanel_;
  private JPanel emptyPanel_;
  private double midFrac_;
  private AnimatedSplitPaneLayoutManager asplLM_;
  private UIComponentSource uics_;
  private DataAccessContext dacx_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public AnimatedSplitPane(UIComponentSource uics, DataAccessContext dacx, AnimatedSplitPaneLayoutManager.PanelForSplit newTopComponent, 
                           AnimatedSplitPaneLayoutManager.PanelForSplit newBottomComponent, AnimatedSplitListener asl) {
    super();
    uics_ = uics;
    dacx_ = dacx;
    asplLM_ = new AnimatedSplitPaneLayoutManager();
    setLayout(asplLM_);
    myTop_ = newTopComponent;
    myBottom_ = newBottomComponent;
    myBottom_.setToHide(true);
    barPanel_ = new JPanel();
    barPanel_.setBackground(Color.LIGHT_GRAY.darker());
    emptyPanel_ = new JPanel();
    emptyPanel_.setLayout(new GridLayout(1, 1));
    JLabel warning = new JLabel(dacx_.getRMan().getString("asp.tooSmallToDisplay"), JLabel.CENTER);
    warning.setFont(dacx_.getFontManager().getFixedFont(FontManager.WORKSHEET_TITLES_LARGE));
    emptyPanel_.add(warning);
    add(myTop_, new AnimatedSplitPaneLayoutManager.ASPRole(AnimatedSplitPaneLayoutManager.IS_TOP));
    add(barPanel_, new AnimatedSplitPaneLayoutManager.ASPRole(AnimatedSplitPaneLayoutManager.IS_BAR));
    add(myBottom_, new AnimatedSplitPaneLayoutManager.ASPRole(AnimatedSplitPaneLayoutManager.IS_BOTTOM));
    add(emptyPanel_, new AnimatedSplitPaneLayoutManager.ASPRole(AnimatedSplitPaneLayoutManager.IS_CARD));
    midFrac_ = 0.5;
    asplLM_.setState(new AnimatedSplitPaneLayoutManager.ASPState(false, 1.0));
    currSplit_ = 1.0;
    inc_ = USE_INC_;
    doFollowOn_ = false;
    // 24 frames a second:
    zoomTimer_ = new Timer(42, this);
    asl_ = asl;
    setEnabled(false);
  }  

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Expand the window
  */

   public void expand() { 
     currSplit_ = 1.0;
     inc_ = -USE_INC_;
     doFollowOn_ = false;
     zoomTimer_.start();
     return;
   } 
  
  /***************************************************************************
  **
  ** Collapse the window
  */

   public void collapse() { 
     currSplit_ = midFrac_;
     inc_ = USE_INC_;
     doFollowOn_ = false;
     zoomTimer_.start();
     return;
   } 
   
  /***************************************************************************
  **
  ** Collapse the window
  */

   public void collapseThenExpand() { 
     currSplit_ = midFrac_;
     inc_ = USE_INC_;
     doFollowOn_ = true;
     zoomTimer_.start();
     return;
   } 
        
  /***************************************************************************
  **
  ** Animation callback
  */ 
  
  public void actionPerformed(ActionEvent ev) {
    try {
      if (currSplit_ > 1.0) {
        currSplit_ = 1.0;
      }   
      if (currSplit_ < midFrac_) {
        currSplit_ = midFrac_;
      }
      asplLM_.setState(new AnimatedSplitPaneLayoutManager.ASPState(true, currSplit_));
      if (inc_ > 0.0) {
        if (currSplit_ == 1.0) {
          if (doFollowOn_) {
            if (asl_ != null) asl_.midBounce();
            inc_ = -USE_INC_;
            doFollowOn_ = false;
          } else {
            zoomTimer_.stop();
            if (asl_ != null) asl_.finished();
            asplLM_.setState(new AnimatedSplitPaneLayoutManager.ASPState(false, currSplit_));
            myBottom_.setToHide(true);
          }
        }
      } else if (inc_ < 0.0) {
        if (currSplit_ == midFrac_) {
          zoomTimer_.stop();
          if (asl_ != null) asl_.finished();
          asplLM_.setState(new AnimatedSplitPaneLayoutManager.ASPState(false, currSplit_));
          myBottom_.setToHide(false);
        }
      }   
      currSplit_ += inc_; // Note this gets out of sync, right??
      revalidate();
      repaint();
    } catch (Exception ex) {
      uics_.getExceptionHandler().displayException(ex);
    }
  }
  
  /***************************************************************************
  **
  ** Old 1.4 bug makes it hard to get the initial scroll position set.  Do
  ** this to fix it.
  */ 
  
  public void doFixie() {
    Dimension size = getSize();
    Dimension rmin = myBottom_.getTrueMinimumSize();
    double useHeight = rmin.height * 1.2;
    midFrac_ = 1.0 - (useHeight / size.height);
    if (midFrac_ < 0.33) {  
      midFrac_ = 0.33;
    }
    asplLM_.setState(new AnimatedSplitPaneLayoutManager.ASPState(false, 1.0));
    return;
  }
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Allows us to call back the owner during phases of the animation
  */
  
  public interface AnimatedSplitListener  {    
    public void midBounce();
    public void finished();
  }
}
