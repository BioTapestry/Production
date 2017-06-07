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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.systemsbiology.biotapestry.cmd.flow.ServerControlFlowHarness;
import org.systemsbiology.biotapestry.genome.GenomeInstance;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogBuildArgs;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogFactory;
import org.systemsbiology.biotapestry.ui.dialogs.factory.DialogPlatform;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTTransmitResultsDialog;
import org.systemsbiology.biotapestry.util.DataUtil;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.SimpleUserFeedback;

/****************************************************************************
**
** Factory for dialog boxes for creating groups via drawing
*/

public class DrawGroupCreationDialogFactory extends DialogFactory {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public DrawGroupCreationDialogFactory(ServerControlFlowHarness cfh) {
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
   
    DrawGroupBuildArgs dniba = (DrawGroupBuildArgs)ba;
    
    GenomeInstance gi = (GenomeInstance)ba.getGenome();    
    Set<String> namesTaken = gi.getVfgParentRoot().usedGroupNames();  
    if (platform.getPlatform() == DialogPlatform.Plat.DESKTOP) {
      return (new DesktopDialog(cfh, buildCombo(gi), dniba.defaultName, namesTaken));
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

  /***************************************************************************
  **
  ** Build the combo of available groups
  ** 
  */
   
   private ArrayList<ObjChoiceContent> buildCombo(GenomeInstance gi) {
     TreeMap<String, ObjChoiceContent> tm = new TreeMap<String, ObjChoiceContent>();
     //
     // List consists of all groups in parent that are not already
     // in this submodel:
     //      
     GenomeInstance rgi = gi.getVfgParentRoot();
     int genCount = gi.getGeneration();  
     Iterator<Group> grit = rgi.getGroupIterator();
     while (grit.hasNext()) {
       Group grp = grit.next();
       String baseID = Group.getBaseID(grp.getID());
       String inherit = Group.buildInheritedID(baseID, genCount);
       if (gi.getGroup(inherit) == null) {
         tm.put(grp.getName(), new ObjChoiceContent(grp.getDisplayName(), grp.getID()));
       }
     }
     ArrayList<ObjChoiceContent> retval = new ArrayList<ObjChoiceContent>(tm.values());
     return (retval);
   }

  ////////////////////////////////////////////////////////////////////////////
  //
  // BUILD ARGUMENT CLASS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  public static class DrawGroupBuildArgs extends DialogBuildArgs { 
    
    String defaultName;
          
    public DrawGroupBuildArgs(GenomeInstance gi, String defaultName) {
      super(gi);
      this.defaultName = defaultName;
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
    private JComboBox importCombo_;
    private ArrayList<ObjChoiceContent> imports_;
    private JRadioButton drawNew_;
    private JRadioButton drawOld_;
    private JLabel nameLabel_;
    private JLabel importLabel_;
    private Set<String> namesTaken_;
    
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
    
    public DesktopDialog(ServerControlFlowHarness cfh, ArrayList<ObjChoiceContent> imports, String defaultName, Set<String> namesTaken) {      
      super(cfh, "createGroupSub.drawTitle", new Dimension(900, 250), 3, new GroupCreationRequest(), false);
      imports_ = imports;
      namesTaken_ = namesTaken;
      
      JLabel label = new JLabel(rMan_.getString("createGroupSub.chooseSource"));
      drawNew_ = new JRadioButton(rMan_.getString("createGroupSub.drawNew"), true);
      drawOld_ = new JRadioButton(rMan_.getString("createGroupSub.drawOld"), false);
            
      ButtonGroup group = new ButtonGroup();
      group.add(drawNew_);
      group.add(drawOld_);
      
      ButtonTracker bt = new ButtonTracker();     
      drawNew_.addActionListener(bt);
      drawOld_.addActionListener(bt);
      addLabeledButtonPair(label, drawNew_, drawOld_);
      
      //
      // Build the name panel:
      //
  
      nameLabel_ = new JLabel(rMan_.getString("createGroupSub.name"));
      nameField_ = new JTextField(defaultName);
      nameField_.selectAll();
      addLabeledWidget(nameLabel_, nameField_, false, false);
      
      addWindowListener(new WindowAdapter() {
        public void windowOpened(WindowEvent e) {
          nameField_.requestFocus();
        }
      });    
        
      //
      // Build the import panel:
      //
  
      importLabel_ = new JLabel(rMan_.getString("createGroupSub.imports"));

      importCombo_ = new JComboBox(imports_.toArray());
      if (imports_.size() == 0) {
        drawOld_.setEnabled(false);
      }
      addLabeledWidget(importLabel_, importCombo_, false, false);
           
      finishConstruction();
      setActiveFields(true);
    }
  
    public boolean dialogIsModal() {
      return (true);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // INNER CLASSES
    //
    //////////////////////////////////////////////////////////////////////////// 
    
    private class ButtonTracker implements ActionListener {
      public void actionPerformed(ActionEvent ev) {
        try {
          boolean isDrawNew = drawNew_.isSelected();
          setActiveFields(isDrawNew);   
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    }
  
    ////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE VIZ & PRIVATE METHODS
    //
    ////////////////////////////////////////////////////////////////////////////
  
    void setActiveFields(boolean isDrawNew) {
      nameLabel_.setEnabled(isDrawNew);
      importLabel_.setEnabled(!isDrawNew);       
      nameField_.setEnabled(isDrawNew);
      importCombo_.setEnabled(!isDrawNew);
      return;
    }   
    
    /***************************************************************************
    **
    ** Do the bundle 
    */
     
    @Override
    protected boolean bundleForExit(boolean forApply) { 
      GroupCreationRequest crq = (GroupCreationRequest)request_; 
      
      if (drawNew_.isSelected()) {
        crq.nameResult = nameField_.getText().trim();
        //
        // If the name matches an import option, we report it as being an import.
        // If not, we check if it lives in the root.  If it does, it conflicts.
        //      
        int numImp = imports_.size();
        for (int i = 0; i < numImp; i++) {
          ObjChoiceContent occ = (ObjChoiceContent)imports_.get(i);
          if (DataUtil.keysEqual(occ.name, crq.nameResult)) {
            crq.nameResult = null;
            crq.idResult = occ.val;
            String message = rMan_.getString("createGroupSub.notDraw");
            String title = rMan_.getString("createGroupSub.notDrawTitle");
            SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.WARNING, message, title);
            cfh_.showSimpleUserFeedback(suf);
            break;
          }
        }
        //
        // If we have a null result, then we need to insure that we do not match any
        // other group name
        //      
        if (crq.idResult == null) {
          if (DataUtil.containsKey(namesTaken_, crq.nameResult)) {
            String desc = MessageFormat.format(rMan_.getString("createGroup.NameInUse"), 
                                               new Object[] {crq.nameResult});
            String title = rMan_.getString("createGroup.CreationErrorTitle");
            SimpleUserFeedback suf = new SimpleUserFeedback(SimpleUserFeedback.JOP.ERROR, desc, title);
            cfh_.showSimpleUserFeedback(suf);
            return (false);
          } 
        }
      } else {
        crq.nameResult = null;
        crq.idResult = ((ObjChoiceContent)importCombo_.getSelectedItem()).val;
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
  
  public static class GroupCreationRequest implements ServerControlFlowHarness.UserInputs {
    private boolean haveResult;
    public String nameResult;
    public String idResult;
    
    public void clearHaveResults() {
      haveResult = false;
      return;
    }   
    public boolean haveResults() {
      return (haveResult);
    }     
	public void setHasResults() {
		this.haveResult = true;
		return;
	}  
    public boolean isForApply() {
      return (false);
    }
  } 
}
