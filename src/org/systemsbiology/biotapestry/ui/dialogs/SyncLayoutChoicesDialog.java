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
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for choosing options for layout syncs
*/

public class SyncLayoutChoicesDialog extends JDialog {

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

  private JCheckBox compressBox_;
  private JCheckBox directBox_;  
  private JCheckBox groupBox_;  
  private JCheckBox swapPadsBox_; 
  private JComboBox<ChoiceContent> overlayOptionCombo_; 
  
  private boolean giveDirectCopyOption_;
  private boolean haveResult_;
  private boolean directCopy_;
  private boolean doCompress_;
  private boolean keepGroups_;
  private boolean swapPads_;
  private int overlayOption_;
  
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
  
  public SyncLayoutChoicesDialog(UIComponentSource uics, boolean offerDirectCopy, DataAccessContext dacx) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("syncLayoutChoices.title"), true);
    uics_ = uics;
    haveResult_ = false;
    
    ResourceManager rMan = dacx.getRMan();    
    setSize(600, 300);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
   
    //
    // Build the option selection buttons
    //
    giveDirectCopyOption_ = offerDirectCopy;
    directBox_ = new JCheckBox(rMan.getString("syncLayoutChoices.direct"), offerDirectCopy);    
    compressBox_ = new JCheckBox(rMan.getString("syncLayoutChoices.compress"), false);
    groupBox_ = new JCheckBox(rMan.getString("syncLayoutChoices.keepGroups"), true);
    swapPadsBox_ = new JCheckBox(rMan.getString("syncLayoutChoices.swapPads"), true);
    JLabel overlayLabel = new JLabel(rMan.getString("layoutParam.overlayOptions"));   
    Vector<ChoiceContent> relayoutChoices = NetOverlayProperties.getRelayoutOptions(dacx);
    overlayOptionCombo_ = new JComboBox<ChoiceContent>(relayoutChoices);
    overlayOptionCombo_.setSelectedItem(NetOverlayProperties.relayoutForCombo(dacx, NetOverlayProperties.RELAYOUT_SHIFT_AND_RESIZE_SHAPES));    
  
    if (offerDirectCopy) {
      compressBox_.setEnabled(false);
      groupBox_.setEnabled(false);
    } else {
      directBox_.setEnabled(false);
    }
    
    int rowNum = 0;
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(directBox_, gbc);
   
    UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(compressBox_, gbc);
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(groupBox_, gbc);
    
    UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(swapPadsBox_, gbc); 
    
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(overlayLabel, gbc);
    
    UiUtil.gbcSet(gbc, 1, rowNum++, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(overlayOptionCombo_, gbc);
   
    if (offerDirectCopy) {
      directBox_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            compressBox_.setEnabled(!directBox_.isSelected());
            groupBox_.setEnabled(!directBox_.isSelected());
          } catch (Exception ex) {
            uics_.getExceptionHandler().displayException(ex);
          }
        }
      });
    }
    
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            SyncLayoutChoicesDialog.this.setVisible(false);
            SyncLayoutChoicesDialog.this.dispose();
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
            SyncLayoutChoicesDialog.this.setVisible(false);
            SyncLayoutChoicesDialog.this.dispose();
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

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, rowNum, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
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
  ** Find if user wants to do a direct copy:
  ** 
  */
  
  public boolean directCopy() {
    return (directCopy_);
  }  
  
  /***************************************************************************
  **
  ** Find if user wants to compress
  ** 
  */
  
  public boolean doCompress() {
    return (doCompress_);
  }
  
  /***************************************************************************
  **
  ** Find if the user wants to retain general group properties
  ** 
  */
  
  public boolean keepGroups() {
    return (keepGroups_);
  }  
  
  /***************************************************************************
  **
  ** Find if the user wants to swap pads
  ** 
  */
  
  public boolean swapPads() {
    return (swapPads_);
  }   
  
  /***************************************************************************
  **
  ** Get desired overlay option
  ** 
  */
  
  public int getOverlayOption() {
    return (overlayOption_);
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
    if (ok) {
      directCopy_ = (giveDirectCopyOption_) ? directBox_.isSelected() : false;
      keepGroups_ = (directCopy_) ? false : groupBox_.isSelected();
      doCompress_ = (directCopy_) ? false : compressBox_.isSelected();   
      swapPads_ = swapPadsBox_.isSelected(); 
      overlayOption_ = ((ChoiceContent)overlayOptionCombo_.getSelectedItem()).val;
      haveResult_ = true;
      return (true);
    } else {
      haveResult_ = false;
      return (true);
    }
  }
}
