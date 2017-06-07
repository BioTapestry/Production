/*
**    Copyright (C) 2003-2016 Institute for Systems Biology 
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

import java.awt.LayoutManager2;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;

import java.util.HashMap;
import java.util.ArrayList;

/****************************************************************************
**
** Handles layout for a stack of multi-strip charts
*/

public class ChartStackLayoutManager implements LayoutManager2 {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

  private int height_;
  private int minWidth_;  
  private boolean haveCalc_;
  private HashMap<Component, Object> constraints_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTOR
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Null constructor
  */
  
  public ChartStackLayoutManager() {
    height_ = 0;
    minWidth_ = 0;    
    haveCalc_ = false;
    constraints_ = new HashMap<Component, Object>();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Get the component constraint
  */
  
  public StackRole getConstraint(Component comp) {
    return ((StackRole)constraints_.get(comp));
  }
  
  /***************************************************************************
  **
  ** Add a layout component by name.  Do not use
  */
  
  public void addLayoutComponent(String name, Component comp) {
    throw new IllegalArgumentException();
  }

  /***************************************************************************
  **
  ** Add a layout component by constraint
  */
 
  public void addLayoutComponent(Component comp, Object constraint) {
    if (!(constraint instanceof StackRole)) { 
      throw new IllegalArgumentException();
    }
    haveCalc_ = false;
    constraints_.put(comp, constraint);
    return;
  }
      
  /***************************************************************************
  **
  ** Remove the given component
  */
  
  public void removeLayoutComponent(Component comp) {
    haveCalc_ = false;
    constraints_.remove(comp);
    return;
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
    return (preferredLayoutSize(parent));
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
  ** Layout the container
  */    
    
  public void layoutContainer(Container parent) {
   
    Insets insets = parent.getInsets();
    Dimension pDim = parent.getSize();
    int useWidth = pDim.width - (insets.left + insets.right);
    sizeCalcs(parent);
    int currY_ = insets.top;
    
    ArrayList<Component> bars = new ArrayList<Component>();
    int nComps = parent.getComponentCount();
    for (int i = 0; i < nComps; i++) {
      Component comp = parent.getComponent(i);
      StackRole role = (StackRole)constraints_.get(comp);
      if (role == null) {
        throw new IllegalStateException();
      }
      if ((role.isButton) || (role.isSpacer)) {
        // Bump up the size with nulls if needed
        if (bars.size() <= role.position) {
          for (int j = bars.size(); j <= role.position; j++) {
            bars.add(null);
          }
        }
        bars.set(role.position, comp);
      }
    }
    int nBars = bars.size();
    for (int i = 0; i < nBars; i++) {    
      Component comp = bars.get(i);
      if (comp == null) {
        continue;
      }
      Dimension d = comp.getPreferredSize();
      comp.setBounds(insets.left, currY_, useWidth, d.height);
      currY_ += d.height;
      StackRole role = (StackRole)constraints_.get(comp);
      Component target = role.target;
      if ((target != null) && target.isVisible()) {
        d = target.getPreferredSize();
        target.setBounds(insets.left, currY_, useWidth, d.height);
        currY_ += d.height;
      }
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
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Defines a display strip element
  */
  
  public static class StackRole {
    
    public boolean isButton;
    public boolean isSpacer;
    public int position;
    public Component target;

    public StackRole(boolean isButton, int position, Component target) {
      this.isButton = isButton;
      this.isSpacer = false;
      this.position = position;
      this.target = target;     
    }
    
    public StackRole(boolean isSpacer, int position) {
      this.isButton = false;
      this.isSpacer = isSpacer;
      this.position = position;
      this.target = null;     
    }  
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
    
    int nComps = parent.getComponentCount();
    for (int i = 0; i < nComps; i++) {
      Component comp = parent.getComponent(i);
      StackRole role = (StackRole)constraints_.get(comp);
      if (role == null) {
        throw new IllegalStateException();
      }
      if (role.isButton) {
        Dimension d = comp.getPreferredSize();
        Dimension dMin = comp.getMinimumSize();        

        height_ += d.height;
        Component target = role.target;
        d = target.getPreferredSize();
        if (dMin.width > minWidth_) {
          minWidth_ = dMin.width;
        }
        if (target.isVisible()) {
          height_ += d.height;
        }
      }
    }
    haveCalc_ = true;
    return;
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
}
