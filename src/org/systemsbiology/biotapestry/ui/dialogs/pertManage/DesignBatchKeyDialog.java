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

import java.awt.Dimension;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;

/****************************************************************************
**
** Dialog box for designing perturbation batch keys
*/

public class DesignBatchKeyDialog extends BTStashResultsDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JCheckBox useDateBox_;
  private JCheckBox useInvestBox_;
  private JCheckBox useTimeBox_;
  private JCheckBox useConditionBox_;
  private boolean useDate_;  
  private boolean useInvest_;
  private boolean useTime_;
  private boolean useCondition_;
  
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
  
  public DesignBatchKeyDialog(UIComponentSource uics, DataAccessContext dacx) {     
    super(uics, dacx, "designBatchKey.title", new Dimension(500, 300), 6);
  
    useDateBox_ = new JCheckBox(rMan_.getString("designBatchKey.useDate"), true);   
    useInvestBox_ = new JCheckBox(rMan_.getString("designBatchKey.useInvest"), false);
    useTimeBox_ = new JCheckBox(rMan_.getString("designBatchKey.useTime"), false);
    useConditionBox_ = new JCheckBox(rMan_.getString("designBatchKey.useCondition"), false);
    
    addWidgetFullRow(new JLabel(rMan_.getString("designBatchKey.describe")), false);
    addWidgetFullRow(useInvestBox_, false);
    addWidgetFullRow(useDateBox_, false);
    addWidgetFullRow(useTimeBox_, false);
    addWidgetFullRow(useConditionBox_, false);
    addWidgetFullRow(new JLabel(rMan_.getString("designBatchKey.batchAlways"), JLabel.CENTER), false);
    
    finishConstruction();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Answer if we are use the date
  ** 
  */
  
  public boolean useDate() {
    return (useDate_);
  }
  
  /***************************************************************************
  **
  ** Answer if we are to use the investigators
  ** 
  */
  
  public boolean useInvest() {
    return (useInvest_);
  }   
  
  /***************************************************************************
  **
  ** Answer if we are to use the batch (hardwired yes)
  ** 
  */
  
  public boolean useBatch() {
    return (true);
  }  
  
  /***************************************************************************
  **
  ** Answer if we are to use the time
  ** 
  */
  
  public boolean useTime() {
    return (useTime_);
  } 
  
  /***************************************************************************
  **
  ** Answer if we are to use the condition
  ** 
  */
  
  public boolean useCondition() {
    return (useCondition_);
  }   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Do the stashing 
  ** 
  */
  
  protected boolean stashForOK() {
    useDate_ = useDateBox_.isSelected();
    useInvest_ = useInvestBox_.isSelected();
    useTime_ = useTimeBox_.isSelected();
    useCondition_ = useConditionBox_.isSelected();
    return (true);
  }
}
