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
import java.util.Set;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;

/****************************************************************************
**
** Factory for dialog boxes for creating network overlays
*/

public class NetOverlayCreationDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public NetOverlayCreationDialogFactory(ServerControlFlowHarness cfh) {
    super(cfh);
  }  
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // FACTORY METHOD
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Get the appropriate dialog
  */  
  
  public ServerControlFlowHarness.Dialog getDialog(DialogBuildArgs ba) { 
   
    NetOverlayBuildArgs dniba = (NetOverlayBuildArgs)ba;
  
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.existingNames));
    } else if (platform.getPlatform() == DialogPlatform.Plat.WEB) {
      throw new IllegalStateException();
    }
    throw new IllegalArgumentException();
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // COMMON HELPERS
  //
  ////////////////////////////////////////////////////////////////////////////      

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class NetOverlayBuildArgs extends DialogBuildArgs { 
    
    Set<String> existingNames;
          
    public NetOverlayBuildArgs(Set<String> existingNames) {
      super(null);
      this.existingNames = existingNames;
    } 
  }
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // DIALOG CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class DesktopDialog extends BTTransmitResultsDialog { 
 

    ////////////////////////////////////////////////////////////////////////////
    //
    // PRIVATE INSTANCE MEMBERS
    //
    ////////////////////////////////////////////////////////////////////////////  
  
    private JTextField nameField_;
    private JComboBox typeCombo_;
    private JCheckBox hideLinksBox_; 
    private Set<String> existingNames_;
    
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, Set<String> existingNames) {      
      super(cfh, "noverlay.title", new Dimension(500, 300), 3, new NetOverlayCreationRequest(), false);
      existingNames_ = existingNames;
     
      //
      // Build the overlay type
      //
      
      JLabel label = new JLabel(rMan_.getString("noverlay.type"));
      Vector<TrueObjChoiceContent> choices = NetOverlayProperties.getOverlayTypes(appState_);
      typeCombo_ = new JComboBox(choices);
      addLabeledWidget(label, typeCombo_, false, false);
      
      //
      // Build the name panel:
      //
  
      label = new JLabel(rMan_.getString("noverlay.name"));
      nameField_ = new JTextField(uniqueNewName(existingNames_));
      addLabeledWidget(label, nameField_, false, false);
  
      //
      // Build the hide links box
      //
  
      hideLinksBox_ = new JCheckBox(rMan_.getString("noverlay.hideLinks"));
      addWidgetFullRow(hideLinksBox_, true); 
     
      finishConstruction();
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PROTECTED/PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Build a unique name
    ** 
    */
    
    private String uniqueNewName(Set<String> existingNames) {
      ResourceManager rMan = appState_.getRMan();
      String uniqueBase = rMan.getString("noverlay.defaultBaseName");
      int count = 1;
      while (true) {
        String newName = uniqueBase + " " + count++;
        if (!DataUtil.containsKey(existingNames, newName)) {
          return (newName);
        }
      }
    }
  
    /***************************************************************************
    **
    ** Do the bundle 
    ** 
    */
     
    @Override   
    protected boolean bundleForExit(boolean forApply) {
      NetOverlayCreationRequest nocrq = (NetOverlayCreationRequest)request_;           
      nocrq.nameResult = nameField_.getText().trim();     
   
      if (nocrq.nameResult.equals("")) {
        String message = rMan_.getString("noverlay.emptyName"); 
        String title = rMan_.getString("noverlay.emptyNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }      
  
      if (DataUtil.containsKey(existingNames_, nocrq.nameResult)) {
        String message = rMan_.getString("noverlay.dupName"); 
        String title = rMan_.getString("noverlay.dupNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, message, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }      
  
      TrueObjChoiceContent typeSelection = (TrueObjChoiceContent)typeCombo_.getSelectedItem();
      nocrq.typeResult = (NetOverlayProperties.OvrType)typeSelection.val;
      nocrq.hideLinksResult = hideLinksBox_.isSelected();
      nocrq.haveResult = true;
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Return Results
  ** 
  */
 
  public static class NetOverlayCreationRequest implements ServerControlFlowHarness.UserInputs {
    private boolean haveResult;
    public String nameResult;
    public NetOverlayProperties.OvrType typeResult;
    public boolean hideLinksResult;

    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
    public boolean haveResults() {
      return (haveResult);
    }  
    public boolean isForApply() {
      return (false);
    }
  }
}
