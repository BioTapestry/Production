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

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.DataMapSource;
import org.systemsbiology.biotapestry.db.ExperimentalDataSource;
import org.systemsbiology.biotapestry.genome.DBGene;
import org.systemsbiology.biotapestry.genome.DBGenome;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.perturb.PerturbationDataMaps;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseDataMaps;
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
  private DataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoSupport support_;
  private boolean userCancelled_;
  private PerturbationData pd_;
  private TemporalInputRangeData tird_;
  private TimeCourseData tcd_;
  private DataMapSource dms_;
  private ExperimentalDataSource eds_;
  private PerturbationDataMaps pdms_; 
  private TimeCourseDataMaps tcdm_;
  
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
  
  public NameChangeChoicesDialog(UIComponentSource uics, DataAccessContext dacx, String selectedID, String oldName, String newName, UndoSupport support) {
    super(uics.getTopFrame(), dacx.getRMan().getString("nameChange.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    
    eds_ = dacx_.getExpDataSrc();
    pd_ = eds_.getPertData();
    tird_ = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
    tcd_ = eds_.getTimeCourseData();
    dms_ = dacx_.getDataMapSrc();
    DBGenome genome = dacx_.getDBGenome();
    pdms_ = dms_.getPerturbationDataMaps();
    tcdm_ = dms_.getTimeCourseDataMaps();
    
    
    
    selectedID_ = GenomeItemInstance.getBaseID(selectedID);
    newName_ = newName;
    oldName_ = oldName;
    support_ = support;
    userCancelled_ = false;

    ResourceManager rMan = uics_.getRMan();
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

   
    DBGene gene = genome.getGeneWithName(oldName_);
    int numNodes = genome.getNodesWithName(oldName_).size();
    if (gene != null) {
      numNodes++;
    }
    
    //
    // If we are sharing experimental data across tabs, we do not provide the option of changing the
    // data name. (User could change name in underlying data, then fix up each tab as needed with gene
    // name changes if they so desired.)
    //
    
    boolean amSharing = eds_.amUsingSharedExperimentalData();
    
    JLabel cannotChangeLabel = (amSharing) ? new JLabel(rMan.getString("nameChange.cannotChange")) : null;  
 
    if (!newName.trim().equals("") && (numNodes == 1)) {
      desc = MessageFormat.format(rMan.getString("nameChange.changeDataName"), new Object[]{oldName_, newName_});
      changeDataName_ = new JRadioButton(desc, true);
      if (amSharing) {
        changeDataName_.setEnabled(false);
      }
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
    
    if ((changeDataName_ != null) && amSharing) {
      UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
      cp.add(cannotChangeLabel, gbc);
      createCustomMap_.setSelected(true); // select second guy
    }
    
    if (changeDataName_ != null) {
      UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(changeDataName_, gbc);
      if (!amSharing) {
        changeDataName_.setSelected(true); // select first guy
      }    
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
          uics_.getExceptionHandler().displayException(ex);
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

    //
    // Perturbation Data
    //
    ArrayList<String> eList = new ArrayList<String>();
    if (pd_.isTargetName(oldName_)) {
      eList.add(pd_.getTargKeyFromName(oldName_));
    }
    ArrayList<String> sList = new ArrayList<String>();
    if (pd_.isSourceName(oldName_)) {
      sList.add(pd_.getSourceKeyFromName(oldName_));
    }
    
   
    PertDataChange[] pdc = pdms_.addDataMaps(selectedID_, eList, sList);
    for (int i = 0; i < pdc.length; i++) {
      support_.addEdit(new PertDataChangeCmd(pdc[i]));
    }

    //
    // Temporal Input
    //

    eList = new ArrayList<String>();
    if (tird_.getRange(oldName_) != null) {
      eList.add(oldName_);
    }
    sList = new ArrayList<String>();
    if (tird_.isPerturbationSourceName(oldName_)) {
      sList.add(oldName_);
    }
    TemporalInputChange[] tic = tird_.addTemporalInputRangeMaps(selectedID_, eList, sList);
    for (int i = 0; i < tic.length; i++) {
      support_.addEdit(new TemporalInputChangeCmd(dacx_, tic[i]));
    }

    //
    // Time Course
    //

    ArrayList<TimeCourseDataMaps.TCMapping> nList = new ArrayList<TimeCourseDataMaps.TCMapping>();
    if (tcd_.getTimeCourseData(oldName_) != null) {
      nList.add(new TimeCourseDataMaps.TCMapping(oldName_));
    }
    TimeCourseChange tcc = tcdm_.addTimeCourseTCMMap(selectedID_, nList, true);
    if (tcc != null) {
      support_.addEdit(new TimeCourseChangeCmd(tcc));
    }
    return (true);
  }

  /***************************************************************************
  **
  ** Handle changing data entries
  ** 
  */
  
  private boolean handleDataNameChanges() {

    boolean amSharing = eds_.amUsingSharedExperimentalData();
    if (amSharing) {
      throw new IllegalStateException();
    }
    
    if (!DataUtil.keysEqual(oldName_,  newName_)) {
      if (pd_.isSourceName(newName_) ||
          pd_.isTargetName(newName_) ||
          ((tird_ != null) && (tird_.getRange(newName_) != null)) ||
          ((tird_ != null) && tird_.isPerturbationSourceName(newName_)) ||
          ((tcd_ != null) && (tcd_.getTimeCourseData(newName_) != null))) {
        ResourceManager rMan = uics_.getRMan();
        JOptionPane.showMessageDialog(uics_.getTopFrame(),
                rMan.getString("nameChange.nameCollision"),
                rMan.getString("nameChange.nameChangeFailureTitle"),
                JOptionPane.ERROR_MESSAGE);
        return (false);
      }
    }

    ArrayList<PerturbationDataMaps> pdmsl = new ArrayList<PerturbationDataMaps>();
    pdmsl.add(pdms_);
    
    PertDataChange[] changes = pd_.changeName(oldName_, newName_, pdmsl, dacx_.getGenomeSource().getRootDBGenome(), dacx_.getExpDataSrc());
    for (int i = 0; i < changes.length; i++) {
      support_.addEdit(new PertDataChangeCmd(changes[i]));
    }

    if (tird_ != null) {
      TemporalInputChange[] tic = tird_.changeName(oldName_, newName_);
      for (int i = 0; i < tic.length; i++) {
        support_.addEdit(new TemporalInputChangeCmd(dacx_, tic[i]));
      }
    }

    if (tcd_ != null) {
      ArrayList<TimeCourseDataMaps> tcdml = new ArrayList<TimeCourseDataMaps>();
      UiUtil.fixMePrintout("NOT ENOUGH NEED ALL MAPS FROM ALL TABS!");
      tcdml.add(tcdm_);
      TimeCourseChange[] tcc = tcd_.changeName(oldName_, newName_, tcdml);
      for (int i = 0; i < tcc.length; i++) {
        support_.addEdit(new TimeCourseChangeCmd(tcc[i]));
      }
    }

    return (true);
  }
}
