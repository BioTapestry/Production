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


package org.systemsbiology.biotapestry.ui.dialogs.utils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.text.MessageFormat;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.ui.dialogs.TimeAxisSetupDialog;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;

/****************************************************************************
**
** Helper for Time Axis handling
*/

public class TimeAxisHelper {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private DataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  
  private JLabel minFieldLabel_;
  private JLabel maxFieldLabel_;
  private JDialog dialog_; 
   
  private JTextField minField_;
  private JTextField maxField_;
  private boolean enabled_;
  private int minTime_;
  private int maxTime_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public TimeAxisHelper(UIComponentSource uics, DataAccessContext dacx, JDialog dialog, JLabel minFieldLabel, JLabel maxFieldLabel, UndoFactory uFac) {
    uics_ = uics;
    dacx_ = dacx;
    uFac_ = uFac;
    minFieldLabel_ = minFieldLabel;
    maxFieldLabel_ = maxFieldLabel;
    dialog_ = dialog;
  }    

  /***************************************************************************
  **
  ** Constructor for panel building
  */ 
  
  public TimeAxisHelper(UIComponentSource uics, DataAccessContext dacx, JDialog dialog, int minTime, int maxTime, UndoFactory uFac) {
    uics_ = uics;
    dacx_ = dacx;
    uFac_ = uFac;
    dialog_ = dialog;
    enabled_ = true;
    minTime_ = minTime;
    maxTime_ = maxTime;
  }  
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  /***************************************************************************
  **
  ** Handle time axis establishment
  ** 
  */
  
  public boolean establishTimeAxis() {  
    TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      TimeAxisSetupDialog tasd = TimeAxisSetupDialog.timeAxisSetupDialogWrapper(uics_, dacx_, uFac_);
      tasd.setVisible(true);
    } else {
      return (true);
    }
    
    tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
    if (!tad.isInitialized()) {
      ResourceManager rMan = dacx_.getRMan();
      JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                    rMan.getString("gicreate.noTimeDefinition"), 
                                    rMan.getString("gicreate.noTimeDefinitionTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    }
    
    fixMinMaxLabels(true);
    return (true);
  }

  /***************************************************************************
  **
  ** Bring time axis labels up to speed
  ** 
  */
  
  public void fixMinMaxLabels(boolean doValidate) {  
    TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
    ResourceManager rMan = dacx_.getRMan();
    String minLabel;
    String maxLabel; 
    if (!tad.isInitialized()) {
      minLabel = rMan.getString("gicreate.undefinedMin");
      maxLabel = rMan.getString("gicreate.undefinedMax");
    } else {
      String displayUnits = tad.unitDisplayString();
      minLabel = MessageFormat.format(rMan.getString("gicreate.minTimeUnitFormat"), new Object[] {displayUnits});
      maxLabel = MessageFormat.format(rMan.getString("gicreate.maxTimeUnitFormat"), new Object[] {displayUnits});
    }
    
    minFieldLabel_.setText(minLabel);
    maxFieldLabel_.setText(maxLabel);
    if (doValidate) {
      minFieldLabel_.invalidate();
      maxFieldLabel_.invalidate();
      dialog_.validate();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Translate time value
  ** 
  */
    
  public String timeValToDisplay(int time) {
    TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
    if (tad.haveNamedStages()) {
      return (tad.getNamedStageForIndex(time).name);
    } else {      
      return (Integer.toString(time));
    }  
  }
  
  /***************************************************************************
  **
  ** Translate time value
  ** 
  */
    
  public int timeDisplayToIndex(String display) {
    TimeAxisDefinition tad = dacx_.getExpDataSrc().getTimeAxisDefinition();
    ResourceManager rMan = dacx_.getRMan();
    int timeVal = TimeAxisDefinition.INVALID_STAGE_NAME;
    if (tad.haveNamedStages()) {
      timeVal = tad.getIndexForNamedStage(display);
      if (timeVal == TimeAxisDefinition.INVALID_STAGE_NAME) {
        JOptionPane.showMessageDialog(dialog_, rMan.getString("gicreate.badStageName"),
                                      rMan.getString("gicreate.badStageNameTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (TimeAxisDefinition.INVALID_STAGE_NAME);
      }   
    } else {
      boolean badNum = false;
      try {
        timeVal = Integer.parseInt(display);
        if (timeVal < 0) {
          badNum = true;
        }
      } catch (NumberFormatException nfex) {
        badNum = true;
      }
      if (badNum) {
        JOptionPane.showMessageDialog(dialog_, rMan.getString("gicreate.badNumber"),
                                      rMan.getString("gicreate.badNumberTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        return (TimeAxisDefinition.INVALID_STAGE_NAME);
      }
    }
    return (timeVal);
  }
  
  /***************************************************************************
  **
  ** Build a helper panel
  ** 
  */
    
  public JPanel buildHelperPanel() {

    JPanel retval = new JPanel();
    GridBagConstraints gbc = new GridBagConstraints();
    retval.setLayout(new GridBagLayout());
    
    //
    // Build the minimum and maxiumum  time:
    //

    minFieldLabel_ = new JLabel("");
    maxFieldLabel_ = new JLabel("");
    fixMinMaxLabels(false);
    
    minField_ = (enabled_) ? new JTextField(timeValToDisplay(minTime_)) : new JTextField();
    minFieldLabel_.setEnabled(enabled_);
    minField_.setEnabled(enabled_);    
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    retval.add(minFieldLabel_, gbc);

    UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(minField_, gbc);
    
    maxField_ = (enabled_) ? new JTextField(timeValToDisplay(maxTime_)) : new JTextField();    
    maxFieldLabel_.setEnabled(enabled_);
    maxField_.setEnabled(enabled_);
    
    UiUtil.gbcSet(gbc, 2, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    retval.add(maxFieldLabel_, gbc);
    
    UiUtil.gbcSet(gbc, 3, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(maxField_, gbc); 
  
    return (retval);
  }
  
  /***************************************************************************
  **
  ** get a result
  ** 
  */
    
  public MinMax getSpanResult() {
    int minResult = timeDisplayToIndex(minField_.getText());
    if (minResult == TimeAxisDefinition.INVALID_STAGE_NAME) {
      return (null);  
    }
    int maxResult = timeDisplayToIndex(maxField_.getText());
    if (maxResult == TimeAxisDefinition.INVALID_STAGE_NAME) {
      return (null);  
    }
    // Gotta be a span:
    if ((minResult >= maxResult) || (minResult < 0)) {
      ResourceManager rMan = dacx_.getRMan(); 
      JOptionPane.showMessageDialog(dialog_, rMan.getString("timeAxisHelper.badBounds"),
                                    rMan.getString("timeAxisHelper.badBoundsTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
          
    }
    return (new MinMax(minResult, maxResult));
  }
}
