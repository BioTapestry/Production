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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
 
/****************************************************************************
**
** Dialog box for position a new group
*/

public class GroupPositionDialog extends JDialog implements ActionListener {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private Point2D result_;
  private Point2D directPoint_;
  private OverviewPanel op_;
  private FixedJButton buttonO_;
  private UIComponentSource uics_;
  
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
  
  public GroupPositionDialog(UIComponentSource uics, StaticDataAccessContext dacx, 
                             Rectangle rect, Point2D directPoint) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("groupPos.title"), true);
    uics_ = uics;
    ResourceManager rMan = dacx.getRMan();    
    setSize(640, 520);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    cp.setMinimumSize(new Dimension(200, 200));
    cp.setPreferredSize(new Dimension(200, 200));    
    GridBagConstraints gbc = new GridBagConstraints();    

    //
    // Build the position panel:
    //

    op_ = new OverviewPanel(uics_, rect, (directPoint != null), this);
    op_.setRenderingContext(dacx);
    op_.setMinimumSize(new Dimension(200, 200));
    op_.setPreferredSize(new Dimension(200, 200));
    JScrollPane jsp = new JScrollPane(op_);
    jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(jsp, gbc);
    
    //
    // If offering direct copy, do it now:
    //

    FixedJButton directButton = null;   
    if (directPoint != null) {
      directPoint_ = directPoint;
      directButton = new FixedJButton(rMan.getString("groupPos.matchRoot"));
      directButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            op_.setChosenPoint(directPoint_); 
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
    }
    
    //
    // Build the button panel:
    //

    buttonO_ = new FixedJButton(rMan.getString("groupPos.ok"));
    buttonO_.setEnabled(false);
    buttonO_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          result_ = op_.getChosenPoint();     
          GroupPositionDialog.this.setVisible(false);
          GroupPositionDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("groupPos.close"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          result_ = null;
          GroupPositionDialog.this.setVisible(false);
          GroupPositionDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    Box buttonPanel = Box.createHorizontalBox();
    if (directPoint != null) {
      buttonPanel.add(directButton);
    }
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO_);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, 10, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
  }

  /***************************************************************************
  **
  ** Get the desired location
  */ 
  
  public Point2D getGroupLocation() {
    return (result_); 
  }

  /***************************************************************************
  **
  ** Invoked when an action occurs.
  */
  public void actionPerformed(ActionEvent e) {
    buttonO_.setEnabled(true);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

}
