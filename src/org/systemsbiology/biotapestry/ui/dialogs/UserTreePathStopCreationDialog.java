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
import java.text.MessageFormat;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.nav.UserTreePath;
import org.systemsbiology.biotapestry.nav.UserTreePathController;
import org.systemsbiology.biotapestry.nav.UserTreePathManager;
import org.systemsbiology.biotapestry.nav.UserTreePathStop;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TaggedSet;

/****************************************************************************
**
** Dialog box for creating a stop on a user tree path
*/

public class UserTreePathStopCreationDialog extends BTStashResultsDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JComboBox<ObjChoiceContent> pathCombo_;
  private JComboBox<ObjChoiceContent> modeCombo_;  
  private String insertModeResult_;
  private String pathKey_;
  private JLabel addInstructionsLabel_;
  private String lastPathKey_;
  private BTState appState_;
  
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
  
  public UserTreePathStopCreationDialog(BTState appState) {     
    super(appState, "pathStopCreate.title", new Dimension(600, 300), 3);
    appState_ = appState;
    lastPathKey_ = appState.getPathController().getLastPath();        
   
    //
    // Build the path selection
    //
    
    JLabel label = new JLabel(rMan_.getString("pathStopCreate.paths"));
    UserTreePathManager mgr = appState_.getPathMgr();
    Vector<ObjChoiceContent> pathChoices = mgr.getPathChoices();
    pathCombo_ = new JComboBox<ObjChoiceContent>(pathChoices);
    if (lastPathKey_ != null) {
      ObjChoiceContent occ = mgr.getPathChoice(lastPathKey_);
      pathCombo_.setSelectedItem(occ);
    }
    pathCombo_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          setInstructions();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    });
    
    addLabeledWidget(label, pathCombo_, false, false);
    
        
    //
    // Build the insertion choices:
    //
    
    label = new JLabel(rMan_.getString("pathStopCreate.insertStop"));
    Vector<ObjChoiceContent> modeChoices = UserTreePathController.insertionOptions(appState_);
    modeCombo_ = new JComboBox<ObjChoiceContent>(modeChoices);
    modeCombo_.setSelectedItem(UserTreePathController.choiceForOption(appState_, UserTreePathController.INSERT_END));
    modeCombo_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          setInstructions();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    });
    
    addLabeledWidget(label, modeCombo_, false, false);
    
    //
    // Build the add instructions
    //        
    
    addInstructionsLabel_ = new JLabel("", JLabel.CENTER);
    setInstructions();
    addWidgetFullRow(addInstructionsLabel_, true);
    finishConstruction(); 
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  /***************************************************************************
  **
  ** Get the path key
  ** 
  */
  
  public String getChosenPath() {
    return (pathKey_);
  }  
  
  /***************************************************************************
  **
  ** Get the insertion mode
  ** 
  */
  
  public String getInsertionMode() {
    return (insertModeResult_);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED/PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Set the note about stop location
  ** 
  */
  
  private void setInstructions() { 
    ObjChoiceContent ocMode = (ObjChoiceContent)modeCombo_.getSelectedItem();   
    String instruct;
    if (ocMode.val.equals(UserTreePathController.INSERT_AFTER_CURRENT)) {
      ObjChoiceContent ocPath = (ObjChoiceContent)pathCombo_.getSelectedItem();
      UserTreePathManager mgr = appState_.getPathMgr();
      UserTreePath path = mgr.getPath(ocPath.val);
      int numStops = path.getStopCount();
      Integer lsObj = appState_.getPathController().getLastStop(ocPath.val);
      int newIndexVal = (lsObj == null) ? numStops : lsObj.intValue() + 2; 
      String format = appState_.getRMan().getString("pathStopCreate.insertStopAdvisory");
      instruct = MessageFormat.format(format, new Object[] {new Integer(newIndexVal), new Integer(numStops)});
    } else {
      instruct = "";
    }
    addInstructionsLabel_.setText(instruct);
    validate();
    return;
  }   
    
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  protected boolean stashForOK() {  
    ObjChoiceContent occ = (ObjChoiceContent)pathCombo_.getSelectedItem();
    UserTreePathManager mgr = appState_.getPathMgr();
    UserTreePath path = mgr.getPath(occ.val);
    String genomeID = appState_.getGenome();
    String ovrKey = appState_.getCurrentOverlay();
    TaggedSet mods = appState_.getCurrentNetModules();
    TaggedSet revs = appState_.getRevealedModules();     
    UserTreePathStop newStop = new UserTreePathStop(genomeID, ovrKey, mods, revs);
    if (path.containsStop(newStop)) {
      ResourceManager rMan = appState_.getRMan();
      int cont = JOptionPane.showConfirmDialog(appState_.getTopFrame(), 
                                               rMan.getString("pathStopCreate.insertDuplicateStop"), 
                                               rMan.getString("pathStopCreate.insertDuplicateStopTitle"),
                                               JOptionPane.YES_NO_OPTION);
      if (cont != JOptionPane.YES_OPTION) {
        return (false);
      }
    }         

    pathKey_ = occ.val;
    occ = (ObjChoiceContent)modeCombo_.getSelectedItem();
    insertModeResult_ = occ.val;
    return (true);
  }    
}
