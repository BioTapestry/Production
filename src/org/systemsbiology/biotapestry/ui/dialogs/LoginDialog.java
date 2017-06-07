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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for entering username and password
*/

public class LoginDialog extends JDialog {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField userNameField_;
  private JPasswordField passwordField_;
  private boolean haveResult_;
  private String userName_;
  private char[] passChars_;
  private UIComponentSource uics_; 
  private DataAccessContext dacx_;
  
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
  
  public LoginDialog(UIComponentSource uics, DataAccessContext dacx) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("login.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    haveResult_ = false;
    
    ResourceManager rMan = dacx_.getRMan();    
    setSize(500, 400);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    

    //
    // Build the username panel:
    //

    JLabel label = new JLabel(rMan.getString("login.user"));
    userNameField_ = new JTextField();
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, 0, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(userNameField_, gbc);
    
    //
    // Build the password field:
    //

    label = new JLabel(rMan.getString("login.password"));
    passwordField_ = new JPasswordField();
    passwordField_.setEchoChar('*');    
    
    UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, 1, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(passwordField_, gbc);
    
    //
    // Build the button panel:
    //

    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          stashResults(true);
          LoginDialog.this.setVisible(false);
          LoginDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.cancel"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          stashResults(false);
          LoginDialog.this.setVisible(false);
          LoginDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, 9, 6, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
  }
  
  /***************************************************************************
  **
  ** Get the name
  ** 
  */
  
  public String getUserName() {
    return (userName_);
  }  
  
  /***************************************************************************
  **
  ** Get the password.  Zero the characters when done.
  ** 
  */
  
  public char[] getPassword() {
    return (passChars_);
  }
  
  /***************************************************************************
  **
  ** Answer if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
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

  /***************************************************************************
  **
  ** Extract our UI values
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      userName_ = userNameField_.getText().trim();
      passChars_ = passwordField_.getPassword();
      haveResult_ = true;
      return (true);
    } else {
      userName_ = null;      
      passChars_ = null;
      haveResult_ = false;
      return (true);
    }
  }
}
