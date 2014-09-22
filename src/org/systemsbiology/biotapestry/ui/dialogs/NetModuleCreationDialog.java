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
import java.util.Set;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.ui.NetModuleProperties;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Dialog box for creating non-gene nodes
*/

public class NetModuleCreationDialog extends BTStashResultsDialog {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField nameField_;
  private JLabel addInstructionsLabel_;
  private JCheckBox addNodesBox_;
  private JCheckBox attachToGroupBox_;
  private JComboBox typeCombo_;
  private String nameResult_;
  private boolean doNodeAdd_;
  private boolean gotNodes_;
  private Set existingNames_;
  private int displayTypeResult_;
  private boolean attachToGroup_;
  
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
  
  public NetModuleCreationDialog(BTState appState, Set existingNames, boolean askForAttach, boolean gotNodes) {
    super(appState, "nmodule.title", new Dimension(500, 300), 3);
    existingNames_ = existingNames;
    gotNodes_ = gotNodes;
  
    //
    // Build the module type
    //
    
    JLabel label = new JLabel(rMan_.getString("nmodule.displayType"));
    Vector choices = NetModuleProperties.getDisplayTypes(appState_, gotNodes);
    typeCombo_ = new JComboBox(choices);
    typeCombo_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          setInstructions((ChoiceContent)typeCombo_.getSelectedItem());
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    });
    
    addLabeledWidget(label, typeCombo_, false, false);
    
    //
    // Build the name panel:
    //

    label = new JLabel(rMan_.getString("nmodule.name"));
    nameField_ = new JTextField(uniqueNewName(existingNames_));
    addLabeledWidget(label, nameField_, false, false);
  
    //
    // Build the add nodes box
    //        
    
    addNodesBox_ = new JCheckBox(rMan_.getString("nmodule.addNodes"));
    addNodesBox_.setSelected(gotNodes);
    addWidgetFullRow(addNodesBox_, false);
  
    //
    // Build the (optional) attach to group box
    //        
    
    if (askForAttach) {
      attachToGroupBox_ = new JCheckBox(rMan_.getString("nmodule.attachToGroup"));
      // Default to false.  This is safer:
      //attachToGroupBox_.setSelected(true);
      addWidgetFullRow(attachToGroupBox_, false);
    }
    
    //
    // Build the add instructions
    //        
    
    addInstructionsLabel_ = new JLabel();
    setInstructions((ChoiceContent)typeCombo_.getSelectedItem());
    addWidgetFullRow(addInstructionsLabel_, false);
 
    finishConstruction();
  }


  /***************************************************************************
  **
  ** Get the answer to adding nodes
  ** 
  */
  
  public boolean doNodeAdd() {
    return (doNodeAdd_);
  }  
  
  /***************************************************************************
  **
  ** Get the name result.
  ** 
  */
  
  public String getName() {
    return (nameResult_);
  }
  
  /***************************************************************************
  **
  ** Get the display type result.
  ** 
  */
  
  public int getDisplayType() {
    return (displayTypeResult_);
  }  

  /***************************************************************************
  **
  ** Ask if we should attach to a group.
  ** 
  */
  
  public boolean getAttachToGroup() {
    return (attachToGroup_);
  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Set the type-based add instructions
  ** 
  */
  
  private void setInstructions(ChoiceContent displayType) {
    String instruct =  NetModuleProperties.mapTypeToInstruction(appState_, displayType.val);
    addInstructionsLabel_.setText(instruct);
    addNodesBox_.setEnabled((displayType.val != NetModuleProperties.MEMBERS_ONLY) && gotNodes_);
    validate();
    return;
  }    
  
  /***************************************************************************
  **
  ** Build a unique name
  ** 
  */
  
  private String uniqueNewName(Set existingNames) {
    ResourceManager rMan = appState_.getRMan();
    String uniqueBase = rMan.getString("nmodule.defaultBaseName");
    int count = 1;
    while (true) {
      String newName = uniqueBase + " " + count++;
      if (!DataUtil.containsKey(existingNames, newName)) {
        return (newName);
      }
    }
  }

  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  protected boolean stashForOK() { 
    nameResult_ = nameField_.getText().trim();

    if (nameResult_.equals("")) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                    rMan.getString("nmodule.emptyName"), 
                                    rMan.getString("nmodule.emptyNameTitle"),
                                    JOptionPane.ERROR_MESSAGE); 
      return (false);
    }      


    if (DataUtil.containsKey(existingNames_, nameResult_)) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                    rMan.getString("nmodule.dupName"), 
                                    rMan.getString("nmodule.dupNameTitle"),
                                    JOptionPane.ERROR_MESSAGE); 
      return (false);
    }      

    doNodeAdd_ = addNodesBox_.isSelected();
    ChoiceContent typeSelection = (ChoiceContent)typeCombo_.getSelectedItem();
    displayTypeResult_ = typeSelection.val;
    attachToGroup_ = ((attachToGroupBox_ != null) && attachToGroupBox_.isSelected());
    return (true);
  }    
}
