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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Factory for dialog boxes that take a name and insure it does not belong to 
** a set of existing names
*/

public class SimpleRestrictedNameDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public SimpleRestrictedNameDialogFactory(ServerControlFlowHarness cfh) {
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
   
    RestrictedNameBuildArgs dniba = (RestrictedNameBuildArgs)ba;
    
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, dniba.title, dniba.msg, dniba.defaultName, dniba.forbidden, dniba.blankOK));
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
  
  public static class RestrictedNameBuildArgs extends DialogBuildArgs { 
    
    String title;
    String msg;
    String defaultName;
    Set<String> forbidden;
    boolean blankOK;
          
    public RestrictedNameBuildArgs(String title, String msg, String defaultName, Set<String> forbidden, boolean blankOK) {
      super(null);
      this.title = title;
      this.msg = msg;
      this.defaultName = defaultName;
      this.forbidden = forbidden;
      this.blankOK = blankOK;
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
    private boolean blankOK_;
    
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, String title, String msg, String defaultName, Set<String> namesTaken, boolean blankOK) {      
      super(cfh, title, new Dimension(400, 200), 2, new SimpleNameRequest(), false);
      namesTaken_ = namesTaken;
      blankOK_ = blankOK;
      
      JLabel label = new JLabel(rMan_.getString(msg));
      if (defaultName != null) {
        nameField_ = new JTextField(defaultName);
        nameField_.selectAll();
      } else {
        nameField_ = new JTextField();
      }
      addLabeledWidget(label, nameField_, false, false);
      
      addWindowListener(new WindowAdapter() {
        public void windowOpened(WindowEvent e) {
          nameField_.requestFocus();
        }
      });    
          
      finishConstruction();
    }
 
    /***************************************************************************
    **
    ** Do the bundle 
    */
     
    @Override
    protected boolean bundleForExit(boolean forApply) { 
      SimpleNameRequest crq = (SimpleNameRequest)request_; 
      
      crq.nameResult = nameField_.getText().trim();
      if (!blankOK_ && crq.nameResult.equals("")) {  
        String msg = rMan_.getString("simpleNameInput.blankNameIllegal");
        String title = rMan_.getString("simpleNameInput.blankNameIllegalTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, msg, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }     
      if ((namesTaken_ != null) && DataUtil.containsKey(namesTaken_, crq.nameResult)) {
        String desc = MessageFormat.format(rMan_.getString("simpleNameInput.nameInUse"), 
                                           new Object[] {crq.nameResult});
        String title = rMan_.getString("simpleNameInput.nameInUseTitle");
        SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, desc, title);
        cfh_.showSimpleUserFeedback(suf);
        return (false);
      }     
      crq.haveResult = true;
      return (true);
    }
  }
 
  /***************************************************************************
  **
  ** Return Results
  ** 
  */
  
  public static class SimpleNameRequest implements ServerControlFlowHarness.UserInputs {
    private boolean haveResult;
    public String nameResult;
    
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
