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
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.PertDataChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.perturb.PertDataChange;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.timeCourse.GroupUsage;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

import org.systemsbiology.biotapestry.db.DataAccessContext;

/****************************************************************************
**
** Dialog box for handling name change consequences to data
*/

public class RegionNameChangeChoicesDialog extends JDialog {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JRadioButton changeDataName_;
  private JRadioButton changeDataNameAndNetworks_;
  private JRadioButton createCustomMap_;
  private JRadioButton breakAssociation_;
  private String groupID_;
  private String newName_;
  private String oldName_;  
  private BTState appState_;
  private UndoSupport support_;
  private boolean userCancelled_;
  private GenomeInstance instance_;
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
  
  public RegionNameChangeChoicesDialog(BTState appState, DataAccessContext dacx, String groupID, 
                                       String oldName, String newName, 
                                       GenomeInstance gi, Group region, 
                                       UndoSupport support) {     
    super(appState.getTopFrame(), appState.getRMan().getString("regionNameChange.title"), true);
    appState_ = appState;
    dacx_ = dacx;
    groupID_ = Group.getBaseID(groupID);
    newName_ = newName;
    oldName_ = oldName;
    support_ = support;
    instance_ = gi;
    userCancelled_ = false;
    
    
    ResourceManager rMan = appState_.getRMan();    
    setSize(800, 300);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
   
    //
    // Build the option selection buttons
    //
    
    String desc = MessageFormat.format(rMan.getString("regionNameChange.changeType"), new Object[] {oldName_, newName_});
    JLabel label = new JLabel(desc);
    if (!newName.trim().equals("")) {
      desc = MessageFormat.format(rMan.getString("regionNameChange.changeDataName"), new Object[] {oldName_, newName_});
      changeDataName_ = new JRadioButton(desc, true);
      if (appState_.getDB().hasOtherRegionsAttachedByDefault(gi, region)) {
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
    UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
    cp.add(label, gbc);
    
    if (changeDataName_ != null) {
      UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(changeDataName_, gbc);
      if (appState_.getDB().hasOtherRegionsAttachedByDefault(gi, region)) {
        UiUtil.gbcSet(gbc, 0, rowNum++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
        cp.add(changeDataNameAndNetworks_, gbc);        
      } 
    }
    
    if (changeDataNameAndNetworks_ != null) {
      UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(changeDataNameAndNetworks_, gbc);
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
            RegionNameChangeChoicesDialog.this.setVisible(false);
            RegionNameChangeChoicesDialog.this.dispose();
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
          RegionNameChangeChoicesDialog.this.setVisible(false);
          RegionNameChangeChoicesDialog.this.dispose();
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
      if (!handleDataNameChanges(false)) {
        return (false);
      }
    } else if ((changeDataNameAndNetworks_ != null) && changeDataNameAndNetworks_.isSelected()) {
      if (!handleDataNameChanges(true)) {
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
    // If there are any regions with the old name, add a map to them
    //
    
    TemporalInputRangeData tird = dacx_.getExpDataSrc().getTemporalInputRangeData(); 
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
    
    //
    // Temporal Input
    //
    
    if (DataUtil.containsKey(tird.getRegions(), oldName_)) {
      List<GroupUsage> groupMap = tird.getCustomTemporalRangeGroupKeys(groupID_);
      if (groupMap == null) {
        groupMap = new ArrayList<GroupUsage>();
      }
      groupMap.add(new GroupUsage(oldName_, null));
      TemporalInputChange tic = tird.setTemporalRangeGroupMap(groupID_, groupMap);
      if (tic != null) {
        support_.addEdit(new TemporalInputChangeCmd(appState_, dacx_, tic));      
      }
    }

    //
    // Time Course
    //
    
    if (DataUtil.containsKey(tcd.getRegions(), oldName_)) {
      List<GroupUsage> groupMap = tcd.getCustomTimeCourseGroupKeys(groupID_);
      if (groupMap == null) {
        groupMap = new ArrayList<GroupUsage>();
      }
      groupMap.add(new GroupUsage(oldName_, null));
      TimeCourseChange tcc = tcd.setTimeCourseGroupMap(groupID_, groupMap, true);
      if (tcc != null) {
        support_.addEdit(new TimeCourseChangeCmd(appState_, dacx_, tcc));      
      }
    }
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Handle changing data entries
  ** 
  */
  
  private boolean handleDataNameChanges(boolean doNetworks) {

    TemporalInputRangeData tird = dacx_.getExpDataSrc().getTemporalInputRangeData(); 
    TimeCourseData tcd = dacx_.getExpDataSrc().getTimeCourseData();
    PerturbationData pd = dacx_.getExpDataSrc().getPertData();
    
    if (DataUtil.containsKey(tcd.getRegions(), newName_) ||
        DataUtil.containsKey(tird.getRegions(), newName_)) {
      ResourceManager rMan = appState_.getRMan();
      String desc = MessageFormat.format(rMan.getString("regionNameChange.nameCollision"), new Object[] {newName_});    
      JOptionPane.showMessageDialog(appState_.getTopFrame(), desc,
                                    rMan.getString("regionNameChange.nameCollisionTitle"),
                                    JOptionPane.WARNING_MESSAGE);
      return (false);
    }
    
    //
    // Gotta handle network before changing data so we accurately assess
    // default mappings:
    //
    
    if (doNetworks) {
      handleNetwork();
    }

    TemporalInputChange[] tic = tird.changeRegionName(oldName_, newName_);    
    for (int i = 0; i < tic.length; i++) {
      support_.addEdit(new TemporalInputChangeCmd(appState_, dacx_, tic[i])); 
    }    

    TimeCourseChange[] tcc = tcd.changeRegionName(oldName_, newName_);
    for (int i = 0; i < tcc.length; i++) {
      support_.addEdit(new TimeCourseChangeCmd(appState_, dacx_, tcc[i])); 
    } 
  
    PertDataChange[] pdc = pd.changeRegionNameForRegionRestrictions(oldName_, newName_);
    if (pdc.length > 0) {
      support_.addEdits(PertDataChangeCmd.wrapChanges(appState_, dacx_, pdc));
      support_.addEvent(new GeneralChangeEvent(GeneralChangeEvent.PERTURB_DATA_CHANGE));
    }
    
    return (true);
  }
  
  /***************************************************************************
  **
  ** Handle changing data entries
  ** 
  */
  
  private void handleNetwork() {

    Iterator<GenomeInstance> iit = dacx_.getGenomeSource().getInstanceIterator();
    while (iit.hasNext()) {
      GenomeInstance tgi = iit.next();
      // root instances only
      if (tgi.getVfgParent() != null) {
        continue;
      }
      if (tgi == instance_) {
        continue;
      }
      Iterator<Group> git = tgi.getGroupIterator();
      while (git.hasNext()) {
        Group testGroup = git.next();
        String tgName = testGroup.getName();
        if ((tgName != null) && DataUtil.keysEqual(tgName, oldName_)) {
          if (dacx_.getExpDataSrc().hasRegionAttachedByDefault(testGroup)) {
            GenomeChange chng = new GenomeChange();
            chng.grOrig = new Group(testGroup);
            testGroup.setName(newName_);
            chng.grNew = new Group(testGroup);
            chng.genomeKey = tgi.getID();   
            support_.addEdit(new GenomeChangeCmd(appState_, dacx_, chng));
            // Actually, all child model groups change too. FIX ME?
            support_.addEvent(new ModelChangeEvent(chng.genomeKey, ModelChangeEvent.UNSPECIFIED_CHANGE));
          }
        }
      }
    }
    return;
  }  
}
