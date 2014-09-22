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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.DependencyAnalyzer;
import org.systemsbiology.biotapestry.perturb.MeasureDictionary;
import org.systemsbiology.biotapestry.perturb.MeasureProps;
import org.systemsbiology.biotapestry.perturb.MeasureScale;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.util.BoundedDoubMinMax;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Panel for creating or editing a perturbation measurement type
*/

public class PertMeasureAddOrEditPanel extends AnimatedSplitEditPanel {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private JTextField nameField_;
  private JComboBox nameCombo_;
  
  private JComboBox scaleCombo_;
  private JComboBox mergeScaleCombo_;
  
  private JTextField negThresh_;
  private JComboBox negCombo_;
  
  private JTextField posThresh_;
  private JComboBox posCombo_;
    
  private PerturbationData pd_;
  private String currKey_;
  private String dupName_;
  private HashSet allMerge_;
  private MeasureProps newProps_;
  private PertManageHelper pmh_;
  
  private TreeSet nameOptions_;
  private TreeSet posOptions_;
  private TreeSet negOptions_;
  
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
  
  public PertMeasureAddOrEditPanel(BTState appState, DataAccessContext dacx, JFrame parent, PerturbationData pd, PendingEditTracker pet, String myKey) { 
    super(appState, dacx, parent, pet, myKey, 4);
    pd_ = pd;
    pmh_ = new PertManageHelper(appState, parent, pd, rMan_, gbc_, pet_);

    //
    // Edit panel
    //
    JPanel editPanel = new JPanel();
    editPanel.setLayout(new GridBagLayout());
    int editRow = 0;
    nameField_ = new JTextField();  
    scaleCombo_ = new JComboBox();
    JButton scJump = new JButton(rMan_.getString("pdpe.jump"), pmh_.getJumpIcon());
    scJump.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)scaleCombo_.getSelectedItem();
          String whichRow = (tocc == null) ? null : (String)tocc.val;
          pet_.jumpToRemoteEdit(PertMiscSetupManagePanel.MANAGER_KEY,
                                PertMiscSetupManagePanel.MEAS_SCALE_KEY, whichRow);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });    
    JPanel scCombo = pmh_.componentWithJumpButton(scaleCombo_, scJump);   
    
    editRow = ds_.installLabelJCompPair(editPanel, "pertMeasureEdit.name", nameField_, 
                                        "pertMeasureEdit.scale", scCombo, editRow, 4, true);
     
    negThresh_ = new JTextField(); 
    posThresh_ = new JTextField();  
    editRow = ds_.installLabelJCompPair(editPanel, "pertMeasureEdit.negThresh", negThresh_, 
                                        "pertMeasureEdit.posThresh", posThresh_, editRow, 4, true);
   
    //
    // Merge panel
    //
    
    JPanel mergePanel = new JPanel();
    mergePanel.setLayout(new GridBagLayout());
    int mergeRow = 0;
    nameCombo_ = new JComboBox();
    nameCombo_.setEditable(true);
    mergeScaleCombo_ = new JComboBox();
    JButton scJumpToo = new JButton(rMan_.getString("pdpe.jump"), pmh_.getJumpIcon());
    scJumpToo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TrueObjChoiceContent tocc = (TrueObjChoiceContent)mergeScaleCombo_.getSelectedItem();
          String whichRow = (tocc == null) ? null : (String)tocc.val;
          pet_.jumpToRemoteEdit(PertMiscSetupManagePanel.MANAGER_KEY,
                                PertMiscSetupManagePanel.MEAS_SCALE_KEY, whichRow);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });    
    JPanel scComboToo = pmh_.componentWithJumpButton(mergeScaleCombo_, scJumpToo);   
    
    mergeRow = ds_.installLabelJCompPair(mergePanel, "pertMeasureEdit.name", nameCombo_, 
                                        "pertMeasureEdit.scale", scComboToo, mergeRow, 4, true);     
    negCombo_ = new JComboBox(); 
    posCombo_ = new JComboBox();
    negCombo_.setEditable(true);
    posCombo_.setEditable(true);
    mergeRow = ds_.installLabelJCompPair(mergePanel, "pertMeasureEdit.negThresh", negCombo_, 
                                        "pertMeasureEdit.posThresh", posCombo_, mergeRow, 4, true);   
    makeMultiMode(editPanel, mergePanel, 4);
    finishConstruction();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Set the new measurement props
  ** 
  */
  
  public void setEditMeasureProps(String newKey) { 
    mode_ = EDIT_MODE;
    currKey_ = newKey;
    displayProperties();
    return;
  }  
  
  /***************************************************************************
  **
  ** Set the measurement props for duplication
  ** 
  */
  
  public void setMeasurePropsForDup(String origKey) { 
    mode_ = DUP_MODE;
    currKey_ = origKey;
    MeasureDictionary md = pd_.getMeasureDictionary();
    dupName_ = pmh_.getUnique(md.getMeasurementOptions(), md.getMeasureProps(currKey_).getName());
    displayProperties();
    return;
  }
  
  /***************************************************************************
  **
  ** Set the measurement props for merging
  ** 
  */
  
  public String setMeasurePropsForMerge(List joinKeys) {
    mode_ = MERGE_MODE;
    allMerge_ = new HashSet(joinKeys);
    DependencyAnalyzer da = pd_.getDependencyAnalyzer();
    Map refCounts = da.getAllMeasurePropReferenceCounts();
    currKey_ = pmh_.getMostUsedKey(refCounts, joinKeys);
    MeasureDictionary md = pd_.getMeasureDictionary();
    nameOptions_ = new TreeSet(String.CASE_INSENSITIVE_ORDER);
    posOptions_ = new TreeSet();
    negOptions_ = new TreeSet();
    int numk = joinKeys.size();     
    for (int i = 0; i < numk; i++) {
      String nextJk = (String)joinKeys.get(i);
      MeasureProps mp = md.getMeasureProps(nextJk);
      nameOptions_.add(mp.getName());
      posOptions_.add(mp.getPosThresh());
      negOptions_.add(mp.getNegThresh());
    }
    displayProperties();
    return (currKey_);
  }

  /***************************************************************************
  **
  ** Get the new measurement props
  ** 
  */
  
  public MeasureProps getResult() {
    return (newProps_);
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
    Vector scaleTypes = pd_.getMeasureDictionary().getScaleOptions();
    if (mode_ == MERGE_MODE) {
      UiUtil.replaceComboItems(mergeScaleCombo_, scaleTypes);
      UiUtil.replaceComboItems(nameCombo_, new Vector(nameOptions_));
      UiUtil.replaceComboItems(posCombo_, new Vector(posOptions_));
      UiUtil.replaceComboItems(negCombo_, new Vector(negOptions_));
    } else {
      UiUtil.replaceComboItems(scaleCombo_, scaleTypes);  
    }
    return;
  }

  /***************************************************************************
  **
  ** Install our UI values:
  ** 
  */
  
  protected boolean stashResults() {
 
    boolean badNum = false;
    Double posThresh = null;
    Double negThresh = null;
    MeasureDictionary md = pd_.getMeasureDictionary();
    
    String ptText;
    String ntText;
    if (mode_ == MERGE_MODE) {
      JTextComponent jtc = (JTextComponent)posCombo_.getEditor().getEditorComponent();
      ptText = jtc.getText().trim();
      jtc = (JTextComponent)(negCombo_.getEditor().getEditorComponent());
      ntText = jtc.getText().trim();
    } else {
      ptText = posThresh_.getText().trim();
      ntText = negThresh_.getText().trim();
    }   
 
    try {
      if (!ptText.equals("")) {
        posThresh = Double.valueOf(ptText);
      }    
      if (!ntText.equals("")) {
        negThresh = Double.valueOf(ntText);
      }
    } catch (NumberFormatException nfex) {
      badNum = true;
    }
    
    //
    // Actually, negative thresh just needs to be lower thresh! (think fold change: always positive!)
    //
    
    if ((negThresh != null) && (posThresh != null)) {
      if (negThresh.doubleValue() > posThresh.doubleValue()) {
        badNum = true;
      }
    }
  
    if (badNum) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pertMeasureEdit.badNumber"),
                                    rMan_.getString("pertMeasureEdit.badNumberTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
      
    String nameText;
    if (mode_ == MERGE_MODE) {
      JTextComponent jtc = (JTextComponent)nameCombo_.getEditor().getEditorComponent();
      nameText = jtc.getText().trim();
    } else {
      nameText = nameField_.getText().trim();
    }   
      
    if (nameText.equals("")) {
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pmae.emptyName"),
                                    rMan_.getString("pmae.emptyNameTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }

    JComboBox useCombo = (mode_ == MERGE_MODE) ? mergeScaleCombo_ : scaleCombo_;
    TrueObjChoiceContent tocc = (TrueObjChoiceContent)useCombo.getSelectedItem();
    if (tocc == null) {   
      JOptionPane.showMessageDialog(parent_, rMan_.getString("pmae.emptyScale"),
                                    rMan_.getString("pmae.emptyScaleTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }     
    String newScaleKey = (String)tocc.val;
         
    //
    // Thresholds cannot be illegal values!
    //
    
    MeasureScale ms = md.getMeasureScale(newScaleKey);
    BoundedDoubMinMax illegal = ms.getIllegalRange();
    if (illegal != null) {
      boolean outOfRange = false;
      if (negThresh != null) {
        outOfRange = illegal.contained(negThresh.doubleValue());
      }  
      if (posThresh != null) {
         outOfRange = outOfRange || illegal.contained(posThresh.doubleValue());
      }  
      if (outOfRange) {
        JOptionPane.showMessageDialog(parent_, rMan_.getString("pmae.valueOutOfRangeForScale"),
                                      rMan_.getString("pmae.valueOutOfRangeForScaleTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
         return (false);
      }
    }

    //
    // No duplicate names allowed
    //  
    
    Iterator kit = md.getKeys();
    while (kit.hasNext()) {
      String key = (String)kit.next();
      MeasureProps mp = md.getMeasureProps(key);
      String name = mp.getName();
      if (DataUtil.keysEqual(name, nameText)) {
        boolean problem = false;
        if (mode_ == MERGE_MODE) {
          problem = !allMerge_.contains(key);
        } else {
          problem = (mode_ == DUP_MODE) || (currKey_ == null) || !currKey_.equals(key);
        }
        if (problem) {        
          JOptionPane.showMessageDialog(parent_, rMan_.getString("pmae.dupName"),
                                        rMan_.getString("pmae.dupNameTitle"), 
                                        JOptionPane.ERROR_MESSAGE);
          return (false);
        }
      }
    }
 
    if ((currKey_ == null) || (mode_ == DUP_MODE)) {
      String nextKey = md.getNextDataKey();   
      newProps_ = new MeasureProps(nextKey, nameText, newScaleKey, negThresh, posThresh);
    } else {
      MeasureProps currProps = md.getMeasureProps(currKey_);
      newProps_ = (MeasureProps)currProps.clone();     
      newProps_.setName(nameText);
      newProps_.setScaleKey(newScaleKey);  
      newProps_.setPosThresh(posThresh);
      newProps_.setNegThresh(negThresh);
    }
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
   
    MeasureDictionary md = pd_.getMeasureDictionary();
    switch (mode_) {
      case EDIT_MODE:
        if (currKey_ == null) {
          String empty = "";
          nameField_.setText(empty); 
          posThresh_.setText(empty);  
          negThresh_.setText(empty);
          UiUtil.initCombo(scaleCombo_);
          cardLayout_.show(myCard_, EDIT_CARD);
        } else {
          MeasureProps currProps = md.getMeasureProps(currKey_);
          stockEditFields(md, currProps, null);
        }
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case DUP_MODE:
        MeasureProps currProps = md.getMeasureProps(currKey_);
        stockEditFields(md, currProps, dupName_);
        cardLayout_.show(myCard_, EDIT_CARD);
        break;
      case MERGE_MODE:
        currProps = md.getMeasureProps(currKey_);
        stockMergeFields(md, currProps);
        cardLayout_.show(myCard_, MERGE_CARD);
        break;
      default:
        throw new IllegalStateException();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Stock edit fields
  ** 
  */
  
  private void stockEditFields(MeasureDictionary md, MeasureProps currProps, String nameOverride) { 
    String currName = (nameOverride == null) ? currProps.getName() : nameOverride;
    nameField_.setText(currName);
    
    String scalekey = currProps.getScaleKey();
    TrueObjChoiceContent toccScale = md.getScaleChoice(scalekey);
    scaleCombo_.setSelectedItem(toccScale);
     
    Double posThresh = currProps.getPosThresh();
    String posThreshStr = (posThresh == null) ? "" : posThresh.toString();
    posThresh_.setText(posThreshStr);
    
    Double negThresh = currProps.getNegThresh();
    String negThreshStr = (negThresh == null) ? "" : negThresh.toString();
    negThresh_.setText(negThreshStr);
       
    return;
  }

  /***************************************************************************
  **
  ** Stock merge fields
  ** 
  */
  
  private void stockMergeFields(MeasureDictionary md, MeasureProps currProps) { 
    String currName = currProps.getName();
    JTextComponent jtc = (JTextComponent)(nameCombo_.getEditor().getEditorComponent());
    jtc.setText(currName);
      
    String scalekey = currProps.getScaleKey();
    TrueObjChoiceContent toccScale = md.getScaleChoice(scalekey);
    mergeScaleCombo_.setSelectedItem(toccScale);
     
    Double posThresh = currProps.getPosThresh();
    String posThreshStr = (posThresh == null) ? "" : posThresh.toString();
    jtc = (JTextComponent)(posCombo_.getEditor().getEditorComponent());
    jtc.setText(posThreshStr);

    Double negThresh = currProps.getNegThresh();
    String negThreshStr = (negThresh == null) ? "" : negThresh.toString();
    jtc = (JTextComponent)(negCombo_.getEditor().getEditorComponent());
    jtc.setText(negThreshStr);

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
    private String nameFieldText;
    private String negThreshText;
    private String posThreshText;
    private TrueObjChoiceContent scaleComboTocc;
    
    MyFreezeDried() {
      if (mode_ == MERGE_MODE) {
        JTextComponent jtc = (JTextComponent)(nameCombo_.getEditor().getEditorComponent());
        nameFieldText = jtc.getText().trim();
        jtc = (JTextComponent)(posCombo_.getEditor().getEditorComponent());
        posThreshText = jtc.getText().trim();
        jtc = (JTextComponent)(negCombo_.getEditor().getEditorComponent());
        negThreshText = jtc.getText().trim();
        scaleComboTocc = (TrueObjChoiceContent)mergeScaleCombo_.getSelectedItem();
      } else {
        nameFieldText = nameField_.getText();
        negThreshText = negThresh_.getText();
        posThreshText = posThresh_.getText();
        scaleComboTocc = (TrueObjChoiceContent)scaleCombo_.getSelectedItem();
      }
    }
    
    public boolean needToCancel() {
      if (mode_ == MERGE_MODE) {
        closeAction();
        return (true);
      }
      if ((currKey_ != null) && (mode_ == EDIT_MODE)) {
        MeasureDictionary md = pd_.getMeasureDictionary();   
        if (md.getMeasureProps(currKey_) == null) {
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
        jtc = (JTextComponent)(posCombo_.getEditor().getEditorComponent());
        jtc.setText(posThreshText);
        jtc = (JTextComponent)(negCombo_.getEditor().getEditorComponent());
        jtc.setText(negThreshText);
        MeasureDictionary md = pd_.getMeasureDictionary();
        if (scaleComboTocc != null) {
          TrueObjChoiceContent toccScale = md.getScaleChoice((String)scaleComboTocc.val);
          mergeScaleCombo_.setSelectedItem(toccScale);
        }
      } else {
        nameField_.setText(nameFieldText);
        negThresh_.setText(negThreshText);
        posThresh_.setText(posThreshText);     
        MeasureDictionary md = pd_.getMeasureDictionary();
        if (scaleComboTocc != null) {
          TrueObjChoiceContent toccScale = md.getScaleChoice((String)scaleComboTocc.val);
          scaleCombo_.setSelectedItem(toccScale);
        }
      }

      return;
    }
  } 
}
