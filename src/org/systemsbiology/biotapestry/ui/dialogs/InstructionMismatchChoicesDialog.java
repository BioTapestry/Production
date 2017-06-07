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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.instruct.BuildInstructionProcessor;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for handling mismatches in submitted build instructions
*/

public class InstructionMismatchChoicesDialog extends JDialog {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JRadioButton splitInstruct_;
  private JRadioButton changeOtherInstruct_;
  private boolean haveResult_;
  private boolean doSplit_;
  private UIComponentSource uics_; 
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
  
  public InstructionMismatchChoicesDialog(UIComponentSource uics, DataAccessContext dacx, JFrame parent, BuildInstructionProcessor.MatchChecker checker) {     
    super(parent, dacx.getRMan().getString("instructionMismatch.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    haveResult_ = false;
    doSplit_ = false;
    
    ResourceManager rMan = dacx_.getRMan();    
    setSize(550, 300);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
   
    int rowNum = 0;
    if (mustSplit(checker)) {
       String desc = buildMustSplitMessage(rMan, checker);
       JLabel label = new JLabel(desc);            
       UiUtil.gbcSet(gbc, 0, rowNum, 4, 4, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);       
       cp.add(label, gbc);
       rowNum += 4;   
    } else {
      //
      // Build the option selection buttons
      //

      String desc = buildMessage(rMan, checker);
      JLabel label = new JLabel(desc);

      splitInstruct_ = new JRadioButton(rMan.getString("instructionMismatch.splitInstruct"), true);
      changeOtherInstruct_ = new JRadioButton(rMan.getString("instructionMismatch.changeOtherInstruct"), false);    

      ButtonGroup group = new ButtonGroup();
      group.add(splitInstruct_);
      group.add(changeOtherInstruct_);
 
      UiUtil.gbcSet(gbc, 0, rowNum, 4, 4, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);       
      cp.add(label, gbc);
      rowNum += 4;

      UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(splitInstruct_, gbc);

      UiUtil.gbcSet(gbc, 0, rowNum++, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
      cp.add(changeOtherInstruct_, gbc);    
    }
    
    
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            InstructionMismatchChoicesDialog.this.setVisible(false);
            InstructionMismatchChoicesDialog.this.dispose();
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.cancel"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(false)) {  
            InstructionMismatchChoicesDialog.this.setVisible(false);
            InstructionMismatchChoicesDialog.this.dispose();
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
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
    setLocationRelativeTo(parent);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  /***************************************************************************
  **
  ** Answer if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
  }
  
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  public boolean doSplit() {
    return (doSplit_);
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
  ** Stash our results for later interrogation.  If they have an error, pop
  ** up a warning dialog and return false, else return true.
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      doSplit_  = (splitInstruct_ == null) ? true : splitInstruct_.isSelected();
      haveResult_ = true;
      return (true);
    } else {
      haveResult_ = false;
      return (true);
    }
  }    


  /***************************************************************************
  **
  ** Answer if we must split
  ** 
  */
  
  private boolean mustSplit(BuildInstructionProcessor.MatchChecker checker) {  
    return (checker.differentCores.size() > 1);
  }

  /***************************************************************************
  **
  ** Build the message:
  ** 
  */
  
  private String buildMustSplitMessage(ResourceManager rMan, BuildInstructionProcessor.MatchChecker checker) {
    BuildInstructionProcessor.CoreInfo ci = checker.differentCores.get(0);
    BuildInstructionProcessor.CoreInfo ci2 = checker.differentCores.get(1);
    int size = checker.differentCores.size();
    String coDisp = checker.original.displayStringUI(rMan);
    String cicDisp = ci.core.displayStringUI(rMan);
    String ci2cDisp = ci2.core.displayStringUI(rMan);    
        
    String desc;
    if (size > 2) {
      desc = MessageFormat.format(rMan.getString("instructionMismatch.mustSplitDescription"), 
                                  new Object[] {coDisp, cicDisp, ci2cDisp, new Integer(size - 2)});
    } else {
      desc = MessageFormat.format(rMan.getString("instructionMismatch.mustSplitDescriptionTwo"), 
                                       new Object[] {coDisp, cicDisp, ci2cDisp});
    }
    return (UiUtil.convertMessageToHtml(desc));
  }  
  
  
  
  /***************************************************************************
  **
  ** Build the message:
  ** 
  */
  
  private String buildMessage(ResourceManager rMan, BuildInstructionProcessor.MatchChecker checker) {
    BuildInstructionProcessor.CoreInfo ci = checker.differentCores.get(0);
    String coDisp = checker.original.displayStringUI(rMan);
    String cicDisp = ci.core.displayStringUI(rMan);
    String desc = MessageFormat.format(rMan.getString("instructionMismatch.description"), 
                                       new Object[] {coDisp, cicDisp});
    return (UiUtil.convertMessageToHtml(desc));
  }
}
