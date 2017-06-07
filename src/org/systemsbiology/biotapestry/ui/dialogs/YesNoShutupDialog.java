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

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.Box;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.RememberSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for general yes/no questions, with an
** option to shut it up
*/

public class YesNoShutupDialog extends JDialog {

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

  private JCheckBox shutUpBox_;
  private boolean haveResult_;
  private boolean confirm_;
  private boolean shutup_;
  private UIComponentSource uics_;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CLASS MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
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
  
  public YesNoShutupDialog(UIComponentSource uics, DataAccessContext dacx, JFrame parent, String titleKey, String message) {     
    super(parent, dacx.getRMan().getString(titleKey), true);
    haveResult_ = false;
    uics_ = uics;
    
    ResourceManager rMan = dacx.getRMan();    
    setSize(600, 250);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
   
    //
    // Add the label
    //

    message = UiUtil.convertMessageToHtml(message);
    
    JLabel showLabel = new JLabel(message, JLabel.CENTER);        
    int rowNum = 0;
    UiUtil.gbcSet(gbc, 0, rowNum, 4, 3, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    rowNum += 3;
    cp.add(showLabel, gbc);
    
    shutUpBox_ = new JCheckBox(rMan.getString("yesNoShutupDialog.shutup"), false);
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(shutUpBox_, gbc);
    
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            YesNoShutupDialog.this.setVisible(false);
            YesNoShutupDialog.this.dispose();
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.cancel"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(false)) {
            YesNoShutupDialog.this.setVisible(false);
            YesNoShutupDialog.this.dispose();
          }
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
    buttonPanel.add(Box.createHorizontalGlue());     
    
    UiUtil.gbcSet(gbc, 0, rowNum, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);    
        
    setLocationRelativeTo(parent);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CLASS METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Only launch if we are needed
  ** 
  */
  
  public static boolean launchIfNeeded(UIComponentSource uics, DataAccessContext dacx, RememberSource rSrc, JFrame parent, String titleKey, String message, String clientKey) {
    try {
      Boolean shutup = rSrc.getStatus(clientKey);
      if ((shutup != null) && shutup.booleanValue()) {
        return (true);
      }
      
      YesNoShutupDialog yns = new YesNoShutupDialog(uics, dacx, parent, titleKey, message);
      yns.setVisible(true);
      
      if (!yns.haveResult()) {
        return (false);  // Don't stash shutup result; window was dismissed
      }
      rSrc.setStatus(clientKey, yns.pleaseShutup());
      return (yns.confirmed());
    } catch (Exception ex) {
      uics.getExceptionHandler().displayException(ex);
    }
    return (false);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Answer if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
  }  
  
  /***************************************************************************
  **
  ** Answer if we are to continue
  ** 
  */
  
  public boolean confirmed() {
    return (confirm_);
  }   
  
  /***************************************************************************
  **
  ** Answer if we are to shutup
  ** 
  */
  
  public boolean pleaseShutup() {
    return (shutup_);
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
  ** Stash our results for later interrogation. 
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    shutup_ = shutUpBox_.isSelected(); 
    confirm_ = ok;
    haveResult_ = ok;
    return (true);
  }
}
