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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.MessageFormat;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biotapestry.ui.dialogs.utils.DialogSupport;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Dialog panel for handling name change consequences to data
*/

public class RegionNameChangeChoicesPanel extends JPanel implements DialogSupport.DialogSupportClient {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JRadioButton changeDataName_;
  private JRadioButton changeDataNameAndNetworks_;
  private JRadioButton createCustomMap_;
  private JRadioButton breakAssociation_;
  private BTStashResultsDialog owner_;
  private RegionPropertiesDialogFactory.RegionRequest req_;
  private String newName_;
  private String oldName_;  
  private boolean userCancelled_;
  private DialogSupport ds_;
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
  
  public RegionNameChangeChoicesPanel(BTStashResultsDialog owner, UIComponentSource uics, DataAccessContext dacx,
                                      RegionPropertiesDialogFactory.RegionRequest req,
                                      String oldName, String newName, boolean hasOtherReg) {  
    
    uics_ = uics;
    dacx_ = dacx;
    ResourceManager rMan = dacx_.getRMan();
    // Maybe still use this even as an embedded panel?
    String title = rMan.getString("regionNameChange.title");
    req_ = req;
    owner_ = owner;

    newName_ = newName;
    oldName_ = oldName;
    userCancelled_ = false;
    GridBagConstraints gbc = new GridBagConstraints();
    ds_ = new DialogSupport(this, uics_, dacx_, gbc);
    
    setBorder(new EmptyBorder(20, 20, 20, 20));
    setLayout(new GridBagLayout());
 
    //
    // Build the option selection buttons
    //
    
    String desc = MessageFormat.format(rMan.getString("regionNameChange.changeType"), new Object[] {oldName_, newName_});
    JLabel label = new JLabel(desc);
    if (!newName.trim().equals("")) {
      desc = MessageFormat.format(rMan.getString("regionNameChange.changeDataName"), new Object[] {oldName_, newName_});
      changeDataName_ = new JRadioButton(desc, true);
      if (hasOtherReg) {
        desc = MessageFormat.format(rMan.getString("regionNameChange.changeDataNameAndNetwork"), new Object[] {oldName_, newName_});
        changeDataNameAndNetworks_ = new JRadioButton(desc, false);      
      }      
    }
        
    desc = MessageFormat.format(rMan.getString("regionNameChange.createCustomMap"), new Object[] {oldName_});
    createCustomMap_ = new JRadioButton(desc, (changeDataName_ == null));
    desc = MessageFormat.format(rMan.getString("regionNameChange.breakAssociation"), new Object[] {oldName_});
    breakAssociation_ = new JRadioButton(desc, false);    
    
    ButtonGroup group = new ButtonGroup();
    if (changeDataName_ != null) {
      group.add(changeDataName_);
    }
    if (changeDataNameAndNetworks_ != null) {
      group.add(changeDataNameAndNetworks_);
    }
    group.add(createCustomMap_);
    group.add(breakAssociation_);    
    
    int rowNum = 0;
    
    rowNum = ds_.addWidgetFullRow(this, label, false, true, rowNum, 4);
    
    if (changeDataName_ != null) {
      rowNum = ds_.addWidgetFullRow(this, changeDataName_, false, false, rowNum, 4);
    }   
    if (changeDataNameAndNetworks_ != null) {
      rowNum = ds_.addWidgetFullRow(this, changeDataNameAndNetworks_, false, false, rowNum, 4);
    }
    rowNum = ds_.addWidgetFullRow(this, createCustomMap_, false, false, rowNum, 4);
    rowNum = ds_.addWidgetFullRow(this, breakAssociation_, false, false, rowNum, 4);
    ds_.buildAndInstallButtonBox(this, rowNum, 4, false, true);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Standard apply
  ** 
  */
 
  public void applyAction() { 
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
 
  public void okAction() {
    handleNameChange();
    return;   
  }
   
  /***************************************************************************
  **
  ** Standard close
  ** 
  */
  
  public void closeAction() {
    userCancelled_ = true;
    owner_.cancelInputPane();
    return;
  }  
  
  /***************************************************************************
  **
  ** Answer if the user cancelled
  ** 
  */
  
  public boolean userCancelled() {
    return (userCancelled_);
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
  ** Do the name change
  ** 
  */
  
  private void handleNameChange() {
    req_.nameMapping = true;
    req_.handleDataNameChanges = false;
    req_.networksToo = false;
    req_.createCustomMap = false;
    req_.breakAssociation = false;
    
    if ((changeDataName_ != null) && changeDataName_.isSelected()) {
      req_.handleDataNameChanges = true;
      req_.networksToo = false;
    } else if ((changeDataNameAndNetworks_ != null) && changeDataNameAndNetworks_.isSelected()) {
      req_.handleDataNameChanges = true;
      req_.networksToo = true;
    } else if (createCustomMap_.isSelected()) {
      req_.createCustomMap = true;
    } else if (breakAssociation_.isSelected()) {
      req_.breakAssociation = true;
    } else {
      throw new IllegalStateException();
    }
    owner_.okAction();
    return;
  }
}
