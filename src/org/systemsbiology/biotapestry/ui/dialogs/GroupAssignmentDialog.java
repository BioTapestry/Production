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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.flow.add.GroupCreationSupport;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.Group;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for Assigning nodes to groups
*/

public class GroupAssignmentDialog extends JDialog {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JComboBox groupCombo_;
  private BTState appState_;
  private Point2D directCenter_;
  private String result_;
  private boolean newGroup_;
  private Rectangle approxBounds_;
  private UndoSupport support_;
  private DataAccessContext rcx_;
  
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
  
  public GroupAssignmentDialog(BTState appState, DataAccessContext rcx, UndoSupport support, 
                               List<Group> availableGroups, Rectangle approxBounds, Point2D directCenter) {     
    super(appState.getTopFrame(), appState.getRMan().getString("groupassign.title"), true);
    rcx_ = rcx;
    appState_ = appState;
    directCenter_ = directCenter;
    support_ = support;
    newGroup_ = false;
    approxBounds_ = approxBounds;

    ResourceManager rMan = appState_.getRMan();    
    setSize(600, 250);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();     

    //
    // We should NOT be called unless there is somebody in the list.
    //
    
    if (availableGroups.size() == 0) {
      throw new IllegalArgumentException();
    }
    
    //
    // Make the combo box.
    //

    JLabel label = new JLabel(rMan.getString("groupassign.select"));
    Iterator<Group> agit = availableGroups.iterator();
    Vector<TrueObjChoiceContent> choices = new Vector<TrueObjChoiceContent>();
    while (agit.hasNext()) {
      Group group = agit.next();
      TrueObjChoiceContent cmap = new TrueObjChoiceContent(group.getDisplayName(), group);
      choices.add(cmap);
    }
    groupCombo_ = new JComboBox(choices);
    
    //
    // Always present an option to create a new group
    //
    
    FixedJButton button = new FixedJButton(rMan.getString("groupassign.create")); 
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          GroupCreationSupport creation = 
            new GroupCreationSupport(appState_, rcx_, false, approxBounds_, null, directCenter_);
          Group newGroup = creation.handleCreation(support_);
          if (newGroup == null) {
            return;
          }
          TrueObjChoiceContent cmap = new TrueObjChoiceContent(newGroup.getDisplayName(), newGroup);
          groupCombo_.addItem(cmap);
          groupCombo_.setSelectedIndex(groupCombo_.getItemCount() - 1);
          newGroup_ = true;
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(groupCombo_, gbc);
    
    UiUtil.gbcSet(gbc, 1, 1, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);
    cp.add(button, gbc);

    //
    // Build the button panel:
    //
   
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          result_ = ((Group)(((TrueObjChoiceContent)groupCombo_.getSelectedItem()).val)).getID();        
          GroupAssignmentDialog.this.setVisible(false);
          GroupAssignmentDialog.this.dispose();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          GroupAssignmentDialog.this.setVisible(false);
          GroupAssignmentDialog.this.dispose();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
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
    UiUtil.gbcSet(gbc, 0, 2, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState_.getTopFrame());
  }
  
  /***************************************************************************
  **
  ** Get the result
  */
  
  public String getResult() {
    return (result_);
  }
  
  /***************************************************************************
  **
  ** Answer if this is a new group
  */
  
  public boolean isNewGroup() {
    return (newGroup_);
  }  
}
