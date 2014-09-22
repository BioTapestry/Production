/*
**    Copyright (C) 2003-2006 Institute for Systems Biology 
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
import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.border.EmptyBorder;

import javax.swing.JPanel;
import javax.swing.JLabel;

/****************************************************************************
**
** Utility for bounds creation
*/

public class ColorLabelToo extends JPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JPanel colorSwatch_;
  private JLabel label_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public ColorLabelToo(Color col, String name) {

    setBorder(new EmptyBorder(5, 5, 5, 5));    
    setLayout(new GridLayout());
   
    colorSwatch_ = new Swatch();
    colorSwatch_.setBackground(col);
    label_ = new JLabel(name);

    Box swatchPanel = Box.createHorizontalBox();
    swatchPanel.add(Box.createHorizontalStrut(5)); 
    swatchPanel.add(colorSwatch_);
    swatchPanel.add(Box.createHorizontalStrut(10));    
    swatchPanel.add(label_);
    add(swatchPanel);
   
  }   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Set the ghosting
  */
      
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    label_.setEnabled(enabled);
    return;
  }  
  
  
  /***************************************************************************
  **
  ** Set the color values
  */
      
  public void setColorValues(Color col, String name) {
    colorSwatch_.setBackground(col);
    label_.setText(name);
    return;
  }
 
  /***************************************************************************
  **
  ** Hard-sized panel
  */
  
  private static class Swatch extends JPanel {

    public Dimension getPreferredSize() {
      Dimension ps = super.getPreferredSize();
      return (new Dimension(ps.height * 5, ps.height));
    }

    public Dimension getMinimumSize() {
      return (getPreferredSize());
    }

    public Dimension getMaximumSize() {
      return (getPreferredSize());
    }
  }
}
