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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.Database;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.Layout;
import org.systemsbiology.biotapestry.util.CheckBoxList;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for duplicating more than one group, possibly into another top-level model
*/

public class MultiGroupDuplicationDialog extends JDialog {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private CheckBoxList groupsToChoose_;
  private JCheckBox changeModelBox_;
  private JComboBox modelCombo_;
  
  private boolean haveResult_;
  private boolean changeModel_;
  private Set<String> groupsToDupResult_; 
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
  
  public MultiGroupDuplicationDialog(BTState appState, String genomeID, Set<String> groupIDs) {     
    super(appState.getTopFrame(), appState.getRMan().getString("multiGrpDup.title"), true);
    appState_ = appState;
    sourceGenome_ = genomeID;
    haveResult_ = false;
    ResourceManager rMan = appState.getRMan();    
    setSize(500, 500);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
   
    //
    // Build the name panel:
    //

    JLabel label = new JLabel(rMan.getString("multiGrpDup.selectGroups"));
    groupsToChoose_ = new CheckBoxList(buildGroupChoices(genomeID, groupIDs), appState_);
    UiUtil.gbcSet(gbc, 0, 0, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    cp.add(label, gbc);

    JScrollPane jsp = new JScrollPane(groupsToChoose_);
    UiUtil.gbcSet(gbc, 0, 1, 3, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(jsp, gbc);         
    
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

    UiUtil.gbcSet(gbc, 0, 6, 3, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
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
    
    UiUtil.gbcSet(gbc, 0, 7, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    cp.add(label, gbc);
    
    UiUtil.gbcSet(gbc, 1, 7, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    cp.add(modelCombo_, gbc);
    
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            MultiGroupDuplicationDialog.this.setVisible(false);
            MultiGroupDuplicationDialog.this.dispose();
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
          MultiGroupDuplicationDialog.this.setVisible(false);
          MultiGroupDuplicationDialog.this.dispose();
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
    
    UiUtil.gbcSet(gbc, 0, 8, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
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
  ** Get the groups to duplicate result.
  */
  
  public Set<String> getGroupsToDup() {
    return (groupsToDupResult_);
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
  ** Get the list of groups to choose from: 
  */
  
  private Vector<CheckBoxList.ListChoice> buildGroupChoices(String genomeID, Set<String> groupIDs) {
    
    Database db = appState_.getDB();
    GenomeInstance gi = (GenomeInstance)db.getGenome(genomeID);
    Layout lo = appState_.getLayoutForGenomeKey(genomeID);
    // HACK ALERT: Use comparable ObjChoiceContents to sort:
    ArrayList<ObjChoiceContent> sortHack = new ArrayList<ObjChoiceContent>();
    int count = 0; 
    Iterator<String> git = groupIDs.iterator(); 
    while (git.hasNext()) {
      String grID = git.next();
      Group group = gi.getGroup(grID);
      String groupMsg = group.getInheritedDisplayName(gi);
      ObjChoiceContent occ = new ObjChoiceContent(groupMsg, grID);
      sortHack.add(occ);
      count++;
    }
    Collections.sort(sortHack);    
      
    Vector<CheckBoxList.ListChoice> retval = new Vector<CheckBoxList.ListChoice>();
    for (int i = 0; i < count; i++) {
      ObjChoiceContent occ = sortHack.get(i);
      String baseGroupID = Group.getBaseID(occ.val);
      GroupProperties gp = lo.getGroupProperties(baseGroupID);
      Color gCol = gp.getColor(true, db);
      retval.add(new CheckBoxList.ListChoice(occ.val, occ.name, gCol, false));
    }

    return (retval);    
  }
   
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
      
      groupsToDupResult_ = new HashSet<String>();
      ListModel myModel = groupsToChoose_.getModel();
      int numElem = myModel.getSize();
      for (int i = 0; i < numElem; i++) {
        CheckBoxList.ListChoice choice = (CheckBoxList.ListChoice)myModel.getElementAt(i);
        if (choice.isSelected) {
          groupsToDupResult_.add((String)choice.getObject());
        }
      }  
      
      //
      // Gotta choose at least one group to work:
      //
          
      if (groupsToDupResult_.isEmpty()) {
        ResourceManager rMan = appState_.getRMan();
        JOptionPane.showMessageDialog(appState_.getTopFrame(), rMan.getString("multiGrpDup.nothingChosen"), 
                                      rMan.getString("multiGrpDup.nothingChosenTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
      
      haveResult_ = true;
    } else {
      groupsToDupResult_ = null;
      modelIDResult_ = sourceGenome_;
      changeModel_ = false;
      haveResult_ = false;
    }
    return (true);
  }    
}
