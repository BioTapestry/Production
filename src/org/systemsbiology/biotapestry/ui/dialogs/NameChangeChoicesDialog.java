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

package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
 **
 ** Dialog box for handling name change consequences to data
 */

public class NameChangeChoicesDialog extends JDialog {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JRadioButton changeDataName_;
  private JRadioButton createCustomMap_;
  private JRadioButton breakAssociation_;
  private String selectedID_;
  private String newName_;
  private String oldName_;
  private BTState appState_;
  private DataAccessContext dacx_;
  private UndoSupport support_;
  private boolean userCancelled_;
  
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
  
  public NameChangeChoicesDialog(BTState appState, DataAccessContext dacx, String selectedID, String oldName, String newName, UndoSupport support) {
    super(appState.getTopFrame(), appState.getRMan().getString("nameChange.title"), true);
    appState_ = appState;
    dacx_ = dacx;
    selectedID_ = GenomeItemInstance.getBaseID(selectedID);
    newName_ = newName;
    oldName_ = oldName;
    support_ = support;
    userCancelled_ = false;

    ResourceManager rMan = appState_.getRMan();
    setSize(600, 300);
    JPanel cp = (JPanel) getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    //
    // Build the option selection buttons
    //

    String desc = MessageFormat.format(rMan.getString("nameChange.changeType"), new Object[]{oldName_, newName_});
    JLabel label = new JLabel(desc);

    DBGenome genome = (DBGenome)appState_.getDB().getGenome();
    DBGene gene = genome.getGeneWithName(oldName_);
    int numNodes = genome.getNodesWithName(oldName_).size();
    if (gene != null) {
      numNodes++;
    }
    if (!newName.trim().equals("") && (numNodes == 1)) {
      desc = MessageFormat.format(rMan.getString("nameChange.changeDataName"), new Object[]{oldName_, newName_});
      changeDataName_ = new JRadioButton(desc, true);
    }
    desc = MessageFormat.format(rMan.getString("nameChange.createCustomMap"), new Object[]{oldName_});
    createCustomMap_ = new JRadioButton(desc, (changeDataName_ == null));
    desc = MessageFormat.format(rMan.getString("nameChange.breakAssociation"), new Object[]{oldName_});
    breakAssociation_ = new JRadioButton(desc, false);

    ButtonGroup group = new ButtonGroup();
    if (changeDataName_ != null) {
      group.add(changeDataName_);
    }
    group.add(createCustomMap_);
    group.add(breakAssociation_);

    int rowNum = 0;
    UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    cp.add(label, gbc);

    if (changeDataName_ != null) {
      UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(changeDataName_, gbc);
    }

    UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(createCustomMap_, gbc);

    UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(breakAssociation_, gbc);

    //
    // Build the button panel:
    //

    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent ev) {
        try {
          if (handleNameChange()) {
            NameChangeChoicesDialog.this.setVisible(false);
            NameChangeChoicesDialog.this.dispose();
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
          userCancelled_ = true;
          NameChangeChoicesDialog.this.setVisible(false);
          NameChangeChoicesDialog.this.dispose();
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
    UiUtil.gbcSet(gbc, 0, rowNum, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState_.getTopFrame());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
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
  
  private boolean handleNameChange() {

    if ((changeDataName_ != null) && changeDataName_.isSelected()) {
      if (!handleDataNameChanges()) {
        return (false);
      }
    } else if (createCustomMap_.isSelected()) {
      if (!handleCustomMapCreation()) {
        return (false);
      }
    } else if (breakAssociation_.isSelected()) {
      // Nothing needs to be done!
    } else {
      throw new IllegalStateException();
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Handle custom map creation
  ** 
  */
  
  private boolean handleCustomMapCreation() {
    //
    // If there are any entries or sources with the old name, add a map to them
    //    

    PerturbationData pd = dacx_.getExpDataSrc().getPertData();
    TemporalInputRangeData tird = dacx_.getExpDataSrc().getTemporalInputRangeData();
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();

    //
    // Perturbation Data
    //
    ArrayList<String> eList = new ArrayList<String>();
    if (pd.isTargetName(oldName_)) {
      eList.add(pd.getTargKeyFromName(oldName_));
    }
    ArrayList<String> sList = new ArrayList<String>();
    if (pd.isSourceName(oldName_)) {
      sList.add(pd.getSourceKeyFromName(oldName_));
    }
    PertDataChange[] pdc = pd.addDataMaps(selectedID_, eList, sList);
    for (int i = 0; i < pdc.length; i++) {
      support_.addEdit(new PertDataChangeCmd(appState_, dacx_, pdc[i]));
    }

    //
    // Temporal Input
    //

    eList = new ArrayList<String>();
    if (tird.getRange(oldName_) != null) {
      eList.add(oldName_);
    }
    sList = new ArrayList<String>();
    if (tird.isPerturbationSourceName(oldName_)) {
      sList.add(oldName_);
    }
    TemporalInputChange[] tic = tird.addTemporalInputRangeMaps(selectedID_, eList, sList);
    for (int i = 0; i < tic.length; i++) {
      support_.addEdit(new TemporalInputChangeCmd(appState_, dacx_, tic[i]));
    }

    //
    // Time Course
    //

    ArrayList<TimeCourseData.TCMapping> nList = new ArrayList<TimeCourseData.TCMapping>();
    if (tcd.getTimeCourseData(oldName_) != null) {
      nList.add(new TimeCourseData.TCMapping(oldName_));
    }
    TimeCourseChange tcc = tcd.addTimeCourseTCMMap(selectedID_, nList, true);
    if (tcc != null) {
      support_.addEdit(new TimeCourseChangeCmd(appState_, dacx_, tcc));
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Handle changing data entries
  ** 
  */
  
  private boolean handleDataNameChanges() {
    
    PerturbationData pd = dacx_.getExpDataSrc().getPertData();
    TemporalInputRangeData tird = dacx_.getExpDataSrc().getTemporalInputRangeData();
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();

    
    if (!DataUtil.keysEqual(oldName_,  newName_)) {
      if (pd.isSourceName(newName_) ||
          pd.isTargetName(newName_) ||
          ((tird != null) && (tird.getRange(newName_) != null)) ||
          ((tird != null) && tird.isPerturbationSourceName(newName_)) ||
          ((tcd != null) && (tcd.getTimeCourseData(newName_) != null))) {
        ResourceManager rMan = appState_.getRMan();
        JOptionPane.showMessageDialog(appState_.getTopFrame(),
                rMan.getString("nameChange.nameCollision"),
                rMan.getString("nameChange.nameChangeFailureTitle"),
                JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }

    PertDataChange[] changes = pd.changeName(oldName_, newName_);
    for (int i = 0; i < changes.length; i++) {
      support_.addEdit(new PertDataChangeCmd(appState_, dacx_, changes[i]));
    }

    if (tird != null) {
      TemporalInputChange[] tic = tird.changeName(oldName_, newName_);
      for (int i = 0; i < tic.length; i++) {
        support_.addEdit(new TemporalInputChangeCmd(appState_, dacx_, tic[i]));
      }
    }

    if (tcd != null) {
      TimeCourseChange[] tcc = tcd.changeName(oldName_, newName_);
      for (int i = 0; i < tcc.length; i++) {
        support_.addEdit(new TimeCourseChangeCmd(appState_, dacx_, tcc[i]));
      }
    }

    return (true);
  }
}
