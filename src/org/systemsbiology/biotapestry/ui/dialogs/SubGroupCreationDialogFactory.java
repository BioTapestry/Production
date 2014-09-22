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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.ui.GroupProperties;
import org.systemsbiology.biotapestry.ui.dialogs.SimpleRestrictedNameDialogFactory.SimpleNameRequest;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Factory for dialog boxes that define subgroups
*/

public class SubGroupCreationDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SubGroupCreationDialogFactory(ServerControlFlowHarness cfh) {
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
   
    SubGroupBuildArgs dniba = (SubGroupBuildArgs)ba;
    
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.defaultName, dniba.forbidden));
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
  
  public static class SubGroupBuildArgs extends DialogBuildArgs { 
    
    String defaultName;
    Set<String> forbidden;
          
    public SubGroupBuildArgs(String defaultName, Set<String> forbidden) {
      super(null);
      this.defaultName = defaultName;
      this.forbidden = forbidden;
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
    private Set<String> namesTaken_;
    private JComboBox layerCombo_;
     
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
     
    public DesktopDialog(ServerControlFlowHarness cfh, String defaultName, Set<String> namesTaken) {
      super(cfh, "sgcreate.title", new Dimension(500, 200), 2, new SubGroupRequest(), false);
      namesTaken_ = namesTaken;
   
      //
      // Build the name panel:
      //
  
      JLabel label = new JLabel(rMan_.getString("sgcreate.name"));
      nameField_ = new JTextField(defaultName);
      addLabeledWidget(label, nameField_, false, false);
      
      //
      // Build the layer type selection
      //
      
      label = new JLabel(rMan_.getString("sgcreate.layer"));
      Vector<Integer> choices = new Vector<Integer>();
      for (int i = 1; i <= GroupProperties.MAX_SUBGROUP_LAYER; i++) {
        choices.add(new Integer(i));
      }
      layerCombo_ = new JComboBox(choices);
      addLabeledWidget(label, layerCombo_, false, false);
 
      finishConstruction();
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // PROTECTED METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    /***************************************************************************
    **
    ** Do the bundle 
    */
      
    @Override
    protected boolean bundleForExit(boolean forApply) { 
      SubGroupRequest crq = (SubGroupRequest)request_;     
      crq.nameResult = nameField_.getText().trim();
      if (DataUtil.containsKey(namesTaken_, crq.nameResult)) {
        String msg = rMan_.getString("sgcreate.badName");
        String title = rMan_.getString("sgcreate.badNameTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, msg, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }
      Integer layerSelection = (Integer)layerCombo_.getSelectedItem();
      crq.layerResult = layerSelection.intValue();
      crq.haveResult = true;
      return (true);
    } 
  }
  
  /***************************************************************************
   **
   ** Return Results
   ** 
   */
   
   public static class SubGroupRequest implements ServerControlFlowHarness.UserInputs {
     private boolean haveResult;
     public String nameResult;
     public int layerResult;
     
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
