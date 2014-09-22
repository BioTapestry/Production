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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for duplicating a group, possibly into another top-level model
*/

public class GroupDuplicationDialog extends JDialog {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField nameField_;
  private JCheckBox changeModelBox_;
  private JComboBox modelCombo_;
  
  private boolean haveResult_;
  private boolean changeModel_;
  private String nameResult_;
  private String modelIDResult_;
  
  private BTState appState_;
  private String sourceGenome_;
  
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
  
  public GroupDuplicationDialog(BTState appState, String genomeID, String groupID, String defaultName) {     
    super(appState.getTopFrame(), appState.getRMan().getString("grpDup.title"), true);
    appState_ = appState;
    sourceGenome_ = genomeID;
    haveResult_ = false;
    ResourceManager rMan = appState_.getRMan();    
    setSize(500, 200);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
   
    //
    // Build the name panel:
    //

    JLabel label = new JLabel(rMan.getString("grpDup.name"));
    nameField_ = new JTextField(defaultName);
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(nameField_, gbc);         
    
    //
    // Check box to select different model:
    //
     
    changeModelBox_ = new JCheckBox(rMan.getString("grpDup.changeModel"));
    changeModelBox_.setSelected(false);
    changeModelBox_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          boolean enabled = changeModelBox_.isSelected();
          modelCombo_.setEnabled(enabled);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });

    UiUtil.gbcSet(gbc, 0, 1, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(changeModelBox_, gbc);

    //
    // Build the model selection
    //
    
    label = new JLabel(rMan.getString("grpDup.models"));
    Vector<ObjChoiceContent> choices = new FullGenomeHierarchyOracle(appState_).topLevelInstanceModels();
    modelCombo_ = new JComboBox(choices);
    modelCombo_.setEnabled(false);
    int numCh = choices.size();
    for (int i = 0; i < numCh; i++) {
      ObjChoiceContent occ = choices.get(i);
      if (genomeID.equals(occ.val)) {
        modelCombo_.setSelectedIndex(i);
        break;
      }
    }    
    
    UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);
    
    UiUtil.gbcSet(gbc, 1, 2, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(modelCombo_, gbc);
    
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            GroupDuplicationDialog.this.setVisible(false);
            GroupDuplicationDialog.this.dispose();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.cancel"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          stashResults(false);        
          GroupDuplicationDialog.this.setVisible(false);
          GroupDuplicationDialog.this.dispose();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
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
    
    UiUtil.gbcSet(gbc, 0, 3, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState_.getTopFrame());
  }


  /***************************************************************************
  **
  ** Get the target model
  */
  
  public String getTargetModel() {
    return (modelIDResult_);
  }  
  
  /***************************************************************************
  **
  ** Get the name result.
  */
  
  public String getName() {
    return (nameResult_);
  }
  
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
  ** Answer if are to change models
  ** 
  */
  
  public boolean doModelChange() {
    return (changeModel_);
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
  ** Stash our results for later interrogation
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      
      //
      // Figure out if we are changing models:
      //
      
      modelIDResult_ = sourceGenome_;
      changeModel_ = changeModelBox_.isSelected();
      if (changeModel_) {
        ObjChoiceContent modelSelection = (ObjChoiceContent)modelCombo_.getSelectedItem();
        modelIDResult_ = modelSelection.val;
        if (modelIDResult_.equals(sourceGenome_)) {
          changeModel_ = false;
        }
      }
      
      GenomeInstance gi = (GenomeInstance)appState_.getDB().getGenome(modelIDResult_);

      String newName = nameField_.getText().trim();
      
      //
      // Even though we do not restrict non-gene node names, they cannot share
      // names with genes.
      //
          
      if (gi.groupNameInUse(newName)) {
        ResourceManager rMan = appState_.getRMan();    
        String desc = MessageFormat.format(rMan.getString("createGroup.NameInUse"), 
                                           new Object[] {newName});
        JOptionPane.showMessageDialog(appState_.getTopFrame(), desc, 
                                      rMan.getString("createGroup.CreationErrorTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
      
      nameResult_ = newName;
      haveResult_ = true;
    } else {
      nameResult_ = null;
      modelIDResult_ = sourceGenome_;
      changeModel_ = false;
      haveResult_ = false;
    }
    return (true);
  }    
}
