/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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

import java.awt.LayoutManager2;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JPanel;

/****************************************************************************
**
** Handles layout for the animated split pane
*/

public class AnimatedSplitPaneLayoutManager implements LayoutManager2 {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  public static final int IS_TOP    = 0;
  public static final int IS_BAR    = 1;
  public static final int IS_BOTTOM = 2;
  public static final int IS_CARD   = 3;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private static final int BAR_HEIGHT_ = 5;
  
  private int height_;
  private int minHeight_;  
  private int minWidth_;  
  private boolean haveCalc_;
  private ASPState currState_;
  private Component[] comps_;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */
  
  public AnimatedSplitPaneLayoutManager() {
    height_ = 0;
    minHeight_ = 0;
    minWidth_ = 0;    
    haveCalc_ = false;
    comps_ = new Component[4];
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Add a layout component by name.  Do not use
  */
  
  public void addLayoutComponent(String name, Component comp) {
    throw new UnsupportedOperationException();
  }

  /***************************************************************************
  **
  ** Add a layout component by constraint
  */
 
  public void addLayoutComponent(Component comp, Object constraint) {
    if (!(constraint instanceof ASPRole)) { 
      throw new IllegalArgumentException();
    }
    haveCalc_ = false;
    comps_[((ASPRole)constraint).role] = comp;
    return;
  }
      
  /***************************************************************************
  **
  ** Remove the given component
  */
  
  public void removeLayoutComponent(Component comp) {
    throw new UnsupportedOperationException();
  }

  /***************************************************************************
  **
  ** Get the preferred size
  */

  public Dimension preferredLayoutSize(Container parent) {
    sizeCalcs(parent);
    Dimension dim = new Dimension(0, 0);
    Insets insets = parent.getInsets();
    dim.width = minWidth_ + insets.left + insets.right;
    dim.height = height_ + insets.top + insets.bottom;
    return (dim);
  }

  /***************************************************************************
  **
  ** Get the minimum size
  */    

  public Dimension minimumLayoutSize(Container parent) {
    sizeCalcs(parent);
    Dimension dim = new Dimension(0, 0);
    Insets insets = parent.getInsets();
    dim.width = minWidth_ + insets.left + insets.right;
    dim.height = minHeight_ + insets.top + insets.bottom;
    return (dim);
  }

  /***************************************************************************
  **
  ** Get the maximum size
  */    
  
  public Dimension maximumLayoutSize(Container parent)  {
    return (preferredLayoutSize(parent));      
  } 
  
  /***************************************************************************
  **
  ** Set the current state
  */
  
  public void setState(ASPState asps) {
    currState_ = asps;
    haveCalc_ = false;
    return;
  }
 
  /***************************************************************************
  **
  ** Layout the container
  */    
    
  public void layoutContainer(Container parent) {
    Insets insets = parent.getInsets();
    Dimension pDim = parent.getSize();
    int useWidth = pDim.width - (insets.left + insets.right);
    int useHeight = pDim.height - (insets.top + insets.bottom);
    sizeCalcs(parent);
 
    int topMin = comps_[IS_TOP].getMinimumSize().height + 22;
    boolean needFull = ((PanelForSplit)comps_[IS_BOTTOM]).isHiding();
    if (needFull) {
      topMin = ((PanelForSplit)comps_[IS_TOP]).getFullMinimumSize().height + 22;
    }
    int bottomMin = comps_[IS_BOTTOM].getMinimumSize().height;
    int sum = topMin + bottomMin + BAR_HEIGHT_;
    if (sum > useHeight) {    
      comps_[IS_TOP].setVisible(false); 
      comps_[IS_BAR].setVisible(false);
      comps_[IS_BOTTOM].setVisible(false);
      comps_[IS_CARD].setVisible(true);   
      comps_[IS_CARD].setBounds(insets.left, insets.top, useWidth, useHeight);
      haveCalc_ = false;
      return;
    } else {
      comps_[IS_TOP].setVisible(true); 
      comps_[IS_BAR].setVisible(true);
      comps_[IS_BOTTOM].setVisible(currState_.frac != 1.0);
      comps_[IS_CARD].setVisible(false);
    }
    
    int div;
    if (currState_.frac == 1.0) {
      div = useHeight - BAR_HEIGHT_;
    } else if (currState_.isAnimating) {
      div = (int)((double)useHeight * currState_.frac);
    } else {
      div = (int)((double)useHeight * currState_.frac);
      if (topMin > div) {
        div = topMin;
      } else if (bottomMin > (useHeight - div - BAR_HEIGHT_)) {
        div = useHeight - BAR_HEIGHT_ - bottomMin;
      }     
    }
    comps_[IS_TOP].setBounds(insets.left, insets.top, useWidth, div);
    comps_[IS_BAR].setBounds(insets.left, div, useWidth, BAR_HEIGHT_);
    if (currState_.frac != 1.0) {
      comps_[IS_BOTTOM].setBounds(insets.left, div + BAR_HEIGHT_, useWidth, useHeight - div - BAR_HEIGHT_);
    }
    return;
  }

  /***************************************************************************
  **
  ** Get the desired X alignment
  */
    
  public float getLayoutAlignmentX(Container parent) {
    return (0.5F);
  }
  
  /***************************************************************************
  **
  ** Get the desired Y alignment
  */
  
  public float getLayoutAlignmentY(Container parent) {
    return (0.5F);    
  }
    
  /***************************************************************************
  **
  ** Void the current layout
  */
  
  public void invalidateLayout(Container parent) {
    haveCalc_ = false;
    return;
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Does the size calculations
  */  
  
  private void sizeCalcs(Container parent) {    
    if (haveCalc_) {
      return;
    }
    
    height_ = 0;
    minWidth_ = 0;
    minHeight_ = 0;
    
    Dimension d = comps_[IS_TOP].getPreferredSize();
    Dimension dMin = comps_[IS_TOP].getMinimumSize();
    boolean needFull = ((PanelForSplit)comps_[IS_BOTTOM]).isHiding();
    if (needFull) {
      dMin = ((PanelForSplit)comps_[IS_TOP]).getFullMinimumSize();
    }
    height_ += d.height; 
    if (dMin.width > minWidth_) {
      minWidth_ = dMin.width;
    }
    minHeight_ += dMin.height + 22;  // Keeps tables from totally collapsing to no rows;

    height_ += BAR_HEIGHT_;
    minHeight_ += BAR_HEIGHT_;
    
    if (currState_.frac != 1.0) {
      d = comps_[IS_BOTTOM].getPreferredSize();
      dMin = comps_[IS_BOTTOM].getMinimumSize();
      height_ += d.height;
      if (dMin.width > minWidth_) {
        minWidth_ = dMin.width;
      }
      minHeight_ += dMin.height + 22;  // Keeps tables from totally collapsing to no rows;
    }
    haveCalc_ = true;
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Defines an animated split pane element
  */
  
  public static class ASPRole {
    
    public static final int IS_TOP    = 0;
    public static final int IS_BAR    = 1;
    public static final int IS_BOTTOM = 2;
    public static final int IS_CARD   = 3;
    
    public int role;
  
    public ASPRole(int role) {
      this.role = role;
    }
  }  
  
  /***************************************************************************
  **
  ** Defines an animated split pane state
  */
  
  public static class ASPState {
    
    public boolean isAnimating;
    public double frac;
  
    public ASPState(boolean isAnimating, double frac) {
      this.isAnimating = isAnimating;
      this.frac = frac;
    }
  }  
  
  /***************************************************************************
  **
  ** Used to override minimum split
  */

  public static class PanelForSplit extends JPanel {
    
    private JPanel useMe_;
    private boolean hideIt_ = false;
    private Dimension hideDim_;

    public PanelForSplit() {
      hideDim_ = new Dimension(0, 0);
    }
    
    public void countOnlyMe(JPanel useMe) {
      useMe_ = useMe;
      return;
    }
           
    public void setToHide(boolean setToHide) {
      hideIt_ = setToHide;
      return;
    }
    
    public boolean isHiding() {
      return (hideIt_);
    }
    
    
    public Dimension getFullMinimumSize() {
      return (super.getMinimumSize());
    }    
    
    public Dimension getTrueMinimumSize() {
      if (useMe_ == null) {
        return (super.getMinimumSize());
      } else {
        return (useMe_.getMinimumSize());
      }
    }
    
    public Dimension getMinimumSize() {
      if (hideIt_) {
        return (hideDim_);
      } else {
        if (useMe_ == null) {
          return (super.getMinimumSize());
        } else {
          return (useMe_.getMinimumSize());
        }
      }
    }
       
    public Dimension getPreferredSize() {
      if (hideIt_) {
        return (hideDim_);
      } else {
        return (super.getPreferredSize());
      }
    }
     
    public Dimension getMaximumSize() {
     if (hideIt_) {
        return (hideDim_);
      } else {
        return (super.getMaximumSize());
      }
    } 
  }    
}
