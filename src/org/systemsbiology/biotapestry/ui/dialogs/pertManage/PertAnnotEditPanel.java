/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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
import java.util.Map;
import java.util.SortedSet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.PertAnnotations;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.util.DataUtil;

/****************************************************************************
**
** Panel for selecting from available annotations
*/

public class PertAnnotEditPanel extends AnimatedSplitEditPanel {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private JComboBox tagCombo_;
  private JTextField messageField_;
  private JComboBox tagComboMrg_;
  private JComboBox messageCombo_;
  
  private PerturbationData pd_;
  private String currKey_;
  private String copyMsg_;
  private String copyTag_;
  private SortedSet tagOptions_;
  private SortedSet nameOptions_;
  private List allMerge_;
  
  private String tagResult_;
  private String msgResult_;
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
  
  public PertAnnotEditPanel(UIComponentSource uics, DataAccessContext dacx, JFrame parent, PerturbationData pd, PendingEditTracker pet, String myKey) {
    super(uics, dacx, parent, pet, myKey, 5);
    pd_ = pd;
    currKey_ = null;
  
    JPanel editPanel = new JPanel();
    editPanel.setLayout(new GridBagLayout()); 
       
    JLabel tagLabel = new JLabel(rMan_.getString("pertAnnotEdit.tag"));
    tagCombo_ = new JComboBox();
    tagCombo_.setEditable(true);
    UiUtil.gbcSet(gbc_, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    editPanel.add(tagLabel, gbc_);
    UiUtil.gbcSet(gbc_, 1, 0, 4, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);    
    editPanel.add(tagCombo_, gbc_);
    
    JLabel messageLabel = new JLabel(rMan_.getString("pertAnnotEdit.message"));
    messageField_ = new JTextField();
    UiUtil.gbcSet(gbc_, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    editPanel.add(messageLabel, gbc_);
    UiUtil.gbcSet(gbc_, 1, 1, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    editPanel.add(messageField_, gbc_);
    
    JPanel mergePanel = new JPanel();
    mergePanel.setLayout(new GridBagLayout()); 
       
    JLabel tagLabel2 = new JLabel(rMan_.getString("pertAnnotEdit.tag"));
    tagComboMrg_ = new JComboBox();
    tagComboMrg_.setEditable(true);
    UiUtil.gbcSet(gbc_, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    mergePanel.add(tagLabel2, gbc_);
    UiUtil.gbcSet(gbc_, 1, 0, 4, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);    
    mergePanel.add(tagComboMrg_, gbc_);
    
    JLabel messageLabel2 = new JLabel(rMan_.getString("pertAnnotEdit.message"));
    messageCombo_ = new JComboBox();
    messageCombo_.setEditable(true);
    UiUtil.gbcSet(gbc_, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    mergePanel.add(messageLabel2, gbc_);
    UiUtil.gbcSet(gbc_, 1, 1, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    mergePanel.add(messageCombo_, gbc_);
 
    makeMultiMode(editPanel, mergePanel, 2);
    finishConstruction();
  }
  
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  public String getMessageResult() {
    return (msgResult_);
  }
  
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  public String getTagResult() {
    return (tagResult_);
  }

  /***************************************************************************
  **
  ** Set state for edit
  ** 
  */
  
  public void setEditAnnotation(String key) {
    mode_ = EDIT_MODE;
    currKey_ = key;
    displayProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Set state for dup
  ** 
  */
  
  public void setDupAnnotation(String copyTag, String copyName) {
    mode_ = DUP_MODE;
    currKey_ = null;
    copyMsg_ = copyName;
    copyTag_ = copyTag;
    displayProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Set state for merge
  ** 
  */
  
  public void setMergeName(List allKeys, String key, SortedSet nameOptions, SortedSet tagOptions) {
    mode_ = MERGE_MODE;
    currKey_ = key;
    nameOptions_ = nameOptions;
    tagOptions_ = tagOptions;
    allMerge_ = allKeys;
    displayProperties();
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
    
  /***************************************************************************
  **
  ** Get freeze-dried state:
  */ 
  
  protected FreezeDried getFreezeDriedState() {
    return (new MyFreezeDried());
  }  
   
  /***************************************************************************
  **
  ** Update the options in our UI components
  ** 
  */
  
  protected void updateOptions() {
    if (mode_ == MERGE_MODE) {   
      UiUtil.replaceComboItems(tagComboMrg_, new Vector(tagOptions_));
      UiUtil.replaceComboItems(messageCombo_, new Vector(nameOptions_));   
    } else {
      Vector choices = pd_.getPertAnnotations().getAvailableTagChoices();
      UiUtil.replaceComboItems(tagCombo_, choices);
    } 
    return;
  }
 
  /***************************************************************************
  **
  ** Stash our results for later interrogation. 
  ** 
  */
  
  protected boolean stashResults() {
    
    PertAnnotations pa = pd_.getPertAnnotations();
    
    String newTag;
    String newMsg;
    
    if (mode_ == MERGE_MODE) {
      JTextComponent jtc = (JTextComponent)(tagComboMrg_.getEditor().getEditorComponent());
      newTag = jtc.getText().trim();
      jtc = (JTextComponent)(messageCombo_.getEditor().getEditorComponent());
      newMsg = jtc.getText().trim();
    } else {
      JTextComponent jtc = (JTextComponent)(tagCombo_.getEditor().getEditorComponent());
      newTag = jtc.getText().trim(); 
      newMsg = messageField_.getText().trim();
    }
     
    if (newTag.equals("")) {   
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pertAnnotEdit.emptyTag"),
                                    rMan_.getString("pertAnnotEdit.emptyTagTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }

    Map footToKey = pa.getFootTagToKeyMap();
    String matchKey = (String)footToKey.get(DataUtil.normKey(newTag));
    if (matchKey != null) {
      boolean problem = false;
      if (mode_ == MERGE_MODE) {
        problem = !allMerge_.contains(matchKey);
      } else {
        problem = (currKey_ == null) || !currKey_.equals(matchKey);
      }
      if (problem) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("pertAnnotEdit.tagInUse"),
                                      rMan_.getString("pertAnnotEdit.tagInUseTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }
    tagResult_ = newTag;
      
  
    if (newMsg.equals("")) {   
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pertAnnotEdit.emptyMsg"),
                                    rMan_.getString("pertAnnotEdit.emptyMsgTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
    
    String gotIt = pa.messageExists(newMsg);
    if (gotIt != null) {
      boolean problem = false;
      if (mode_ == MERGE_MODE) {
        problem = !allMerge_.contains(gotIt);
      } else {
        problem = (currKey_ == null) || !gotIt.equals(currKey_);
      }
      if (problem) {
        String gotTag = pa.getTag(gotIt);
        String format = rMan_.getString("pertAnnotEdit.dupMessageFormat");
        String desc = MessageFormat.format(format, new Object[] {gotTag}); 
        int ok = 
          JOptionPane.showConfirmDialog(parent_, desc,
                                        rMan_.getString("pertAnnotEdit.dupMessageTitle"),
                                        JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
          return (false);
        }
      }
    }
        
    msgResult_ = newMsg;
    return (true);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Apply the current property values to our UI components
  ** 
  */
  
  private void displayProperties() {
    updateOptions();
    
    PertAnnotations pa = pd_.getPertAnnotations();
    
     switch (mode_) {
      case EDIT_MODE: 
        if (currKey_ == null) {
          UiUtil.initCombo(tagCombo_);
          messageField_.setText("");
          messageField_.setCaretPosition(0); 
        } else {          
          tagCombo_.setSelectedItem(pa.getTag(currKey_));
          messageField_.setText(pa.getMessage(currKey_));
          messageField_.setCaretPosition(0);
        }
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case DUP_MODE:
        JTextComponent jtc = (JTextComponent)(tagCombo_.getEditor().getEditorComponent());
        jtc.setText(copyTag_);
        messageField_.setText(copyMsg_);
        messageField_.setCaretPosition(0);
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case MERGE_MODE:
        jtc = (JTextComponent)(tagComboMrg_.getEditor().getEditorComponent());
        jtc.setText(pa.getTag(currKey_));
        jtc = (JTextComponent)(tagCombo_.getEditor().getEditorComponent());
        jtc.setText(pa.getMessage(currKey_));
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
  ** Freeze dried state for hot updates
  */

  public class MyFreezeDried implements FreezeDried {

    private String messageFieldText;
    private String tagFieldText;
    
    MyFreezeDried() {
      if (mode_ == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)(tagComboMrg_.getEditor().getEditorComponent());
        tagFieldText = jtc.getText().trim();
        jtc = (JTextComponent)(messageCombo_.getEditor().getEditorComponent());
        messageFieldText = jtc.getText().trim();
      } else {
        JTextComponent jtc = (JTextComponent)(tagCombo_.getEditor().getEditorComponent());
        tagFieldText = jtc.getText().trim(); 
        messageFieldText = messageField_.getText().trim();
      }
    }
    
    public boolean needToCancel() {
      if (mode_ == MERGE_MODE) {
        closeAction();
        return (true);
      }
      if (currKey_ != null) {
        if (pd_.getPertAnnotations().getTag(currKey_) == null) {
          closeAction();
          return (true);
        }  
      }
      return (false);
    }
 
    public void reInstall() {
      if (mode_ == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)(tagComboMrg_.getEditor().getEditorComponent());
        jtc.setText(tagFieldText);
        jtc = (JTextComponent)(messageCombo_.getEditor().getEditorComponent());
        jtc.setText(messageFieldText);
      } else {
        messageField_.setText(messageFieldText);
        JTextComponent jtc = (JTextComponent)(tagCombo_.getEditor().getEditorComponent());
        jtc.setText(tagFieldText);
      }
      return;
    }
  }
}
