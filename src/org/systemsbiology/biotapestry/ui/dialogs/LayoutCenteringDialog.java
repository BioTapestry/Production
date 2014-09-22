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


import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;

/****************************************************************************
**
** Dialog box for handling layout centering options
*/

public class LayoutCenteringDialog extends BTStashResultsDialog {

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

  private JRadioButton doCentersButton_;
  private JRadioButton doMatchupsButton_;
  private JCheckBox ignoreOverlaysBox_;  
  private JCheckBox doCenteringBox_;  
  private boolean doMatchups_;  
  private boolean doCenterShift_;    
  private boolean ignoreOverlays_; 
  
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
  
  public LayoutCenteringDialog(BTState appState, boolean emptyRoot, boolean haveOverlays) {
    super(appState, "layoutCenterDialog.title", new Dimension(500, 250), 4);

    //
    // If the root is empty, we must be tracking overlays.  The only choice is whether they want to
    // use overlays for centering:
    //
   
    if (emptyRoot) {
      if (!haveOverlays) {
        throw new IllegalArgumentException();
      }
      ignoreOverlaysBox_ = new JCheckBox(rMan_.getString("layoutCenterDialog.ignoreOverlays"), false);
      addWidgetFullRow(ignoreOverlaysBox_, false);
      
    } else {
      //
      // Build the option to center on bounds
      //
      doCentersButton_ = new JRadioButton(rMan_.getString("layoutCenterDialog.useCenters"), true);
      if (haveOverlays) {
        doCentersButton_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              ignoreOverlaysBox_.setEnabled(doCentersButton_.isSelected());
            } catch (Exception ex) {
              appState_.getExceptionHandler().displayException(ex);
            }
            return;
          }
        });
      } 
      addWidgetFullRow(doCentersButton_, false);
      
      //
      // Check box ignoring overlay bounds
      //

      if (haveOverlays) {
        ignoreOverlaysBox_ = new JCheckBox(rMan_.getString("layoutCenterDialog.ignoreOverlays"), false);
        addWidgetFullRowWithInsets(ignoreOverlaysBox_, false, 5, 35, 5, 5);
      }
      
      //
      // Box for overlaying elements
      //
      
      doMatchupsButton_ = new JRadioButton(rMan_.getString("layoutCenterDialog.useOverlays"), false);
      if (haveOverlays) {
        doMatchupsButton_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              ignoreOverlaysBox_.setEnabled(!doMatchupsButton_.isSelected());
            } catch (Exception ex) {
              appState_.getExceptionHandler().displayException(ex);
            }
            return;
          }
        });
      }
      addWidgetFullRowWithInsets(doMatchupsButton_, false, 5, 5, 20, 5);
      
      ButtonGroup group = new ButtonGroup();
      group.add(doCentersButton_);
      group.add(doMatchupsButton_);    

      //
      // Check box for centering
      //

      doCenteringBox_ = new JCheckBox(rMan_.getString("layoutCenterDialog.doCentering"), true);
      addWidgetFullRow(doCenteringBox_, false);

    }
    
    finishConstruction(); 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Answer if we are to do matchups
  ** 
  */
  
  public boolean doMatchups() {
    return (doMatchups_);
  }
  
  /***************************************************************************
  **
  ** Answer if we are to do centering
  ** 
  */
  
  public boolean doCentering() {
    return (doCenterShift_);
  } 
  
  /***************************************************************************
  **
  ** Answer if we are to ignore overlay bounds in the operation
  ** 
  */
  
  public boolean ignoreOverlays() {
    return (ignoreOverlays_);
  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Stash our results for later interrogation. 
  ** 
  */
  
  protected boolean stashForOK() {
    doMatchups_ = doMatchupsButton_.isSelected();
    doCenterShift_ = doCenteringBox_.isSelected();
    ignoreOverlays_ = (doCenterShift_) && (ignoreOverlaysBox_ != null) && ignoreOverlaysBox_.isSelected();
    return (true);
  }
}
