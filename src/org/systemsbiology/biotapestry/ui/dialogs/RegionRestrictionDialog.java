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

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.Box;
import javax.swing.border.EmptyBorder;
import java.util.Vector;
import java.util.List;

import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.app.BTState;

/****************************************************************************
**
** Dialog box for specifying a region restriction
*/

public class RegionRestrictionDialog extends JDialog {
      
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JComboBox regionCombo_;
  private String regionResult_;
  private List notAllowed_;  
  private boolean haveResult_;
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
  
  public RegionRestrictionDialog(BTState appState, List existingChoices, List notAllowed, String chosen) {     
    super(appState.getTopFrame(), appState.getRMan().getString("regRestrict.title"), true);
    appState_ = appState;
    notAllowed_ = notAllowed;
    ResourceManager rMan = appState_.getRMan();    
    setSize(500, 200);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    haveResult_ = false;
   
    //
    // Build the region selection
    //
    
    JLabel label = new JLabel(rMan.getString("regRestrict.regions"));
    Vector choices = new Vector();
    choices.addAll(existingChoices);
    regionCombo_ = new JComboBox(choices);
    regionCombo_.setEditable(true);
    if (chosen != null) {
      regionCombo_.setSelectedItem(chosen);
    }
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);
    
    UiUtil.gbcSet(gbc, 1, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(regionCombo_, gbc);
                
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            RegionRestrictionDialog.this.setVisible(false);
            RegionRestrictionDialog.this.dispose();
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.cancel"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          stashResults(false);        
          RegionRestrictionDialog.this.setVisible(false);
          RegionRestrictionDialog.this.dispose();
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
  ** Answer if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
  }  
  
  /***************************************************************************
  **
  ** Get the region result
  ** 
  */
  
  public String getRegion() {
    return (regionResult_);
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
  ** Stash our results for later interrogation
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {     
      //
      // Even though we do not restrict non-gene node names, they cannot share
      // names with genes.
      // 
      
      String regionSelection = (String)regionCombo_.getSelectedItem();
      // FIX ME??? Is the selection sometimes not taking when we edit it?  Use this?
      // regionSelection = (String)regionCombo_.getEditor().getItem();
      if ((regionSelection == null) || regionSelection.trim().equals("")) {
        ResourceManager rMan = appState_.getRMan();
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      rMan.getString("regRestrict.emptyName"), 
                                      rMan.getString("regRestrict.emptyNameTitle"),
                                      JOptionPane.ERROR_MESSAGE); 
        return (false);
      } 
      
      if (notAllowed_.contains(regionSelection)) {
        ResourceManager rMan = appState_.getRMan();
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      rMan.getString("regRestrict.dupName"), 
                                      rMan.getString("regRestrict.dupNameTitle"),
                                      JOptionPane.ERROR_MESSAGE); 
        return (false);
      }      

      regionResult_ = regionSelection;
      haveResult_ = true;
    } else {
      haveResult_ = false;
    }
    return (true);
  }    
}
