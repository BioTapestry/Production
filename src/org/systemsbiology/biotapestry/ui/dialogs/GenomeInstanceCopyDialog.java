/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import java.text.MessageFormat;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;

/****************************************************************************
**
** Dialog box for copying genome instances, with a recursive option
*/

public class GenomeInstanceCopyDialog extends BTStashResultsDialog {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JTextField nameField_;
  private JCheckBox recursiveBox_;
  private String nameResult_;
  private boolean isRecursiveResult_;
  
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
  
  public GenomeInstanceCopyDialog(BTState appState, String origName, boolean hasKids) {     
    super(appState, "gicopy.title", new Dimension(500, 200), 4);
   
    //
    // Build the name panel:
    //

    JLabel label = new JLabel(rMan_.getString("gicopy.name"));
    String copyFormat = rMan_.getString("gicopy.newNameFormat");
    String newName = MessageFormat.format(copyFormat, new Object[] {origName});
     
    nameField_ = new JTextField(newName);
    addLabeledWidget(label, nameField_, false, true);
     
    //
    // Recursive copy choice:
    //
    
    recursiveBox_ = new JCheckBox(rMan_.getString("gicopy.doRecursive"));
    recursiveBox_.setSelected(hasKids);
    recursiveBox_.setEnabled(hasKids);
    addWidgetFullRow(recursiveBox_, true);
    
    finishConstruction();
    
  }

  /***************************************************************************
  **
  ** Answer if we should copy recursively
  ** 
  */
  
  public boolean isRecursive() {
    return (isRecursiveResult_);
  }
  
  /***************************************************************************
  **
  ** Get the name result.
  ** 
  */
  
  public String getName() {
    return (nameResult_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Stash our results for later interrogation.
  ** 
  */
  
  protected boolean stashForOK() {  
    nameResult_ = nameField_.getText().trim();
    isRecursiveResult_ = recursiveBox_.isSelected();
    return (true);
  }    
}
