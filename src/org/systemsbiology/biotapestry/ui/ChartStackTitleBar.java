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

package org.systemsbiology.biotapestry.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;

import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.HandlerAndManagerSource;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Title Bar component of  Chart Stack
*/

public class ChartStackTitleBar extends JPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private FixedJButton button_;
  private ImageIcon openedPic_;
  private ImageIcon closedPic_;
  private JLabel label_;
  private Component target_;
  private Container parent_;
  private String uniqueKey_; 
  private HandlerAndManagerSource hams_;
  
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public ChartStackTitleBar(String name, Container parent, Component target, String uniqueKey, HandlerAndManagerSource hams) {
    hams_ = hams;
    setLayout(new GridBagLayout());
    setBorder(new ChartBorder(2));
    GridBagConstraints gbc = new GridBagConstraints();     

    URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/Down16.gif");     
    openedPic_ = new ImageIcon(ugif);
    ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/Forward16.gif");     
    closedPic_ = new ImageIcon(ugif);
    button_ = new FixedJButton(openedPic_);
    button_.setBackground(Color.gray.brighter());
    label_ = new JLabel(name);
    parent_ = parent;
    target_ = target;
    uniqueKey_ = uniqueKey;
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
    add(button_, gbc);
    button_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          boolean isVis = target_.isVisible();
          setOpen(!isVis);
        } catch (Exception ex) {
          hams_.getExceptionHandler().displayException(ex);
        }
      }
    });
    setBackground(Color.gray.brighter());
    UiUtil.gbcSet(gbc, 1, 0, UiUtil.REM, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.E, 1.0, 1.0);
    add(label_, gbc);
  }   

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Answer what our key is
  */

  public String getKey() {
    return (uniqueKey_);
  }
  
  /***************************************************************************
  **
  ** Set our key
  */

  public void setKey(String key) {
    uniqueKey_ = key;
    return;
  }
   
  /***************************************************************************
  **
  ** Answer what our viz is
  */

  public boolean isOpen() {
    return (target_.isVisible());
  }
 
  /***************************************************************************
  **
  ** Open or close the bar
  */

  public void setOpen(boolean isVis) {
    target_.setVisible(isVis);
    button_.setIcon(!isVis ? closedPic_ : openedPic_);
    button_.repaint();
    parent_.validate();
    return;
  }
 
  /***************************************************************************
  **
  ** Disable the bar
  */

  public void disableBar() {
    button_.setEnabled(false);
    return;
  }
  
  /***************************************************************************
  **
  ** Enable the bar
  */

  public void enableBar() {
    button_.setEnabled(true);
    return;
  }

  /***************************************************************************
  **
  ** Set the name
  */

  public void setTitle(String name) {
    label_.setText(name);
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Defines a Chart Border
  */  
      
  public static class ChartBorder extends AbstractBorder {
    
    private int size_;
    private static final long serialVersionUID = 1L;
    
    public ChartBorder(int size) {
      size_ = size;
    }
    
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      g.setColor(Color.black);
      g.drawRect(x, y, width, size_);
      return;
    }

    public Insets getBorderInsets(Component c) {
      return (new Insets(size_, 0, 0, 0));
    }

    public Insets getBorderInsets(Component c, Insets insets) {
      insets.top = size_;
      insets.bottom = 0;
      insets.left = 0;
      insets.right = 0;
      return (insets);
    }
    
    public boolean isBorderOpaque() {
      return (true);
    }
  }  
}
