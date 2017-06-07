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


package org.systemsbiology.biotapestry.ui.dialogs.pertManage;

import java.awt.GridBagLayout;
import java.text.MessageFormat;
import java.util.List;
import java.util.SortedSet;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;

/****************************************************************************
**
** Panel for editing or creating a simple name
*/

public class PertSimpleNameEditPanel extends AnimatedSplitEditPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private String nameKey_;
  private Client client_;
  private String newName_;
  private JTextField nameField_;
  private JComboBox nameCombo_;

  private String copyName_;
  private SortedSet options_;
  private List allMerge_;
  
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
  
  public PertSimpleNameEditPanel(UIComponentSource uics, DataAccessContext dacx, JFrame parent, PendingEditTracker pet, String label, Client client, String myKey) { 
    super(uics, dacx, parent, pet, myKey, 4);
    client_ = client;
    
    JPanel editPanel = new JPanel();
    editPanel.setLayout(new GridBagLayout()); 
    JLabel valueLabel = new JLabel(rMan_.getString(label));
    nameField_ = new JTextField();
    UiUtil.gbcSet(gbc_, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    editPanel.add(valueLabel, gbc_);
    UiUtil.gbcSet(gbc_, 1, 0, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    editPanel.add(nameField_, gbc_);
     
    JPanel mergePanel = new JPanel();
    mergePanel.setLayout(new GridBagLayout()); 
    JLabel valueLabelToo = new JLabel(rMan_.getString(label));
    nameCombo_ = new JComboBox();
    nameCombo_.setEditable(true);
    UiUtil.gbcSet(gbc_, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    mergePanel.add(valueLabelToo, gbc_);
    UiUtil.gbcSet(gbc_, 1, 0, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    mergePanel.add(nameCombo_, gbc_);
      
    makeMultiMode(editPanel, mergePanel, 1);
    finishConstruction();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Set the new simple name
  ** 
  */
  
  public void setEditName(String key) {
    mode_ = EDIT_MODE;
    nameKey_ = key;
    displayProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the new simple name
  ** 
  */
  
  public void setDupName(String copyName) {
    mode_ = DUP_MODE;
    nameKey_ = null;
    copyName_ = copyName;
    displayProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the new simple name.
  ** 
  */
  
  public void setMergeName(String key, SortedSet options, List origKeys) {
    mode_ = MERGE_MODE;
    nameKey_ = key;
    options_ = options;
    allMerge_ = origKeys;
    displayProperties();
    return;
  }
     
  /***************************************************************************
  **
  ** Get the result
  ** 
  */
  
  public String getResult() {
    return (newName_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Update the options in our UI components
  ** 
  */
  
  protected void updateOptions() {
    // nothing to do.
    return;
  }
  
  
  /***************************************************************************
  **
  ** Get freeze-dried state:
  */ 
  
  protected FreezeDried getFreezeDriedState() {
    return (new MyFreezeDried());
  }  
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  protected boolean stashResults() {       
    String newText;
    if (mode_ == MERGE_MODE) {
      JTextComponent jtc = (JTextComponent)(nameCombo_.getEditor().getEditorComponent());
      newText = jtc.getText().trim();
    } else {
      newText = nameField_.getText().trim();
    }   
          
    if ((newText == null) || newText.equals("")) {   
      JOptionPane.showMessageDialog(parent_, rMan_.getString("snep.emptyName"),
                                    rMan_.getString("snep.emptyNameTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }     
    
    if (client_.haveDuplication(myKey_, nameKey_, newText)) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("snep.duplicateName"),
                                    rMan_.getString("snep.duplicateNameTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
    
    if (mode_ == MERGE_MODE) {
      int numDisconnect = 0;
      String exampOldName = null;
      int numM = allMerge_.size();
      for (int i = 0; i < numM; i++) {
        String nextKey = (String)allMerge_.get(i);
        if (client_.haveDisconnect(myKey_, nextKey, newText)) {
          numDisconnect++;
          if (exampOldName == null) {
            exampOldName = client_.getNameForKey(myKey_, nextKey);
          }
        }
      }      
      if (numDisconnect > 0) {
        String desc = UiUtil.convertMessageToHtml(MessageFormat.format(rMan_.getString("snep.disconnectingMultiRow"), 
                                                  new Object[] {new Integer(numDisconnect), exampOldName, newText}));
        int doit = JOptionPane.showConfirmDialog(parent_, desc,
                                                 rMan_.getString("snep.disconnectingTitle"),
                                                 JOptionPane.OK_CANCEL_OPTION);
        if (doit != JOptionPane.OK_OPTION) {
          return (false);
        }
      }
    } else {   
      if (client_.haveDisconnect(myKey_, nameKey_, newText)) {
        String oldName = client_.getNameForKey(myKey_, nameKey_);
        String desc = UiUtil.convertMessageToHtml(MessageFormat.format(rMan_.getString("snep.disconnectingRow"), 
                                                  new Object[] {oldName, newText}));
        int doit = JOptionPane.showConfirmDialog(parent_, desc,
                                                 rMan_.getString("snep.disconnectingTitle"),
                                                 JOptionPane.OK_CANCEL_OPTION);
        if (doit != JOptionPane.OK_OPTION) {
          return (false);
        }
      }
    }
 
    newName_ = newText;
    return (true);
  }
   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties() {
    switch (mode_) {
      case EDIT_MODE: 
        if (nameKey_ == null) {
          nameField_.setText("");
        } else {
          nameField_.setText(client_.getNameForKey(myKey_, nameKey_));
        }
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case DUP_MODE:
        nameField_.setText(copyName_);
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case MERGE_MODE:
        String useName = client_.getNameForKey(myKey_, nameKey_);
        UiUtil.replaceComboItems(nameCombo_, new Vector(options_));
        nameCombo_.setSelectedItem(useName);
        cardLayout_.show(myCard_, MERGE_CARD);
        break;
      default:
        throw new IllegalStateException();
    }
    return;
  }
  

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Clients implement to use the class
  */

  public interface Client {
    public String getNameForKey(String whoAmI, String key);
    public boolean haveDuplication(String whoAmI, String key, String name);
    public boolean haveDisconnect(String whoAmI, String key, String name);
  }
  
  /***************************************************************************
  **
  ** Freeze dried state for hot updates
  */

  private class MyFreezeDried implements FreezeDried {
    
    String nameFieldText;
    
    MyFreezeDried() {
      if (mode_ == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)(nameCombo_.getEditor().getEditorComponent());
        nameFieldText = jtc.getText().trim();
      } else {
        nameFieldText = nameField_.getText().trim();
      } 
    }
    
    public boolean needToCancel() {
      if (mode_ == MERGE_MODE) {
        closeAction();
        return (true);
      }
      if (nameKey_ != null) {
        if (client_.getNameForKey(myKey_, nameKey_) == null) {
          closeAction();
          return (true);
        }  
      }
      return (false);
    } 
 
    public void reInstall() {
      if (mode_ == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)(nameCombo_.getEditor().getEditorComponent());
        jtc.setText(nameFieldText);
      } else {
        nameField_.setText(nameFieldText);
      }     
      return;
    }
  }
}
