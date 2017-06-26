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

import java.awt.Dimension;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;

/****************************************************************************
**
** Dialog box for picking nodes for layout
*/

public class AllowReparentOptimizeDialog extends BTStashResultsDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private boolean allowReparenting_;   
  private JRadioButton baseOption_;
  private JRadioButton reparentOption_;
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
  
  public AllowReparentOptimizeDialog(UIComponentSource uics) {
    super(uics, "allowRepOpt.title", new Dimension(400, 200), 1);
    allowReparenting_ = false;

    ButtonGroup group = new ButtonGroup();
    baseOption_ = new JRadioButton(rMan_.getString("allowRepOpt.baseline"), true);            
    group.add(baseOption_);
    addWidgetFullRow(baseOption_, true, true);    

    reparentOption_ = new JRadioButton(rMan_.getString("allowRepOpt.allow"), false);            
    group.add(reparentOption_);
    addWidgetFullRow(reparentOption_, true, true);    
     
    finishConstruction();

  }  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Get the result.
  ** 
  */
  
  public boolean allowReparenting() {
    return (allowReparenting_);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  protected boolean stashForOK() {    
    allowReparenting_ = reparentOption_.isSelected();
    return (true);
  }
}
