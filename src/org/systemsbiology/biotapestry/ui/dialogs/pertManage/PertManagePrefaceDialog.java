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

package org.systemsbiology.biotapestry.ui.dialogs.pertManage;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.SortedSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box to initialize perturbations management dialog
*/

public class PertManagePrefaceDialog extends JDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JComboBox sourceCombo_;
  private JComboBox targCombo_;
  private boolean haveResult_;
  private PertFilterExpression pertFilterExpr_;
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
  
  public PertManagePrefaceDialog(BTState appState, JFrame parent) {     
    super(parent, appState.getRMan().getString("pertManagePreface.title"), true);
    appState_ = appState;
    haveResult_ = false;
    
    ResourceManager rMan = appState_.getRMan();    
    setSize(600, 350);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();  
    
    JLabel mainLabel = new JLabel(rMan.getString("pertManagePreface.chooseOpFilters"));
    int rowNum = 0;
    UiUtil.gbcSet(gbc, 0, rowNum++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    cp.add(mainLabel, gbc);
     
    PerturbationData pd = appState.getDB().getPertData();
    
    SortedSet srcCand = pd.getCandidates(PertFilter.SOURCE);
    String ncstr = rMan.getString("pertManagePreface.chooseAll");
    Vector srcVec = new Vector();
    srcVec.add(ncstr);
    srcVec.addAll(srcCand);
    
    JLabel srcLabel = new JLabel(rMan.getString("pertManagePreface.chooseSource"));
    sourceCombo_ = new JComboBox(srcVec);
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    cp.add(srcLabel, gbc);
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(sourceCombo_, gbc);    
  
    SortedSet targCand = pd.getCandidates(PertFilter.TARGET);
    Vector targVec = new Vector();
    targVec.add(ncstr);
    targVec.addAll(targCand);
    
    JLabel targLabel = new JLabel(rMan.getString("pertManagePreface.chooseTarg"));
    targCombo_ = new JComboBox(targVec);
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    cp.add(targLabel, gbc);
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(targCombo_, gbc);    
    
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            PertManagePrefaceDialog.this.setVisible(false);
            PertManagePrefaceDialog.this.dispose();
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
          if (stashResults(false)) {
            PertManagePrefaceDialog.this.setVisible(false);
            PertManagePrefaceDialog.this.dispose();
          }
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
  ** Get the pert filter expression
  ** 
  */
  
  public PertFilterExpression getFilterExpr() {
    return (pertFilterExpr_);
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
  ** Stash our results for later interrogation. 
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      
      PertFilter srcFilter;
      if (sourceCombo_.getSelectedIndex() == 0) {
        srcFilter = null;
      } else {
        String useSrc = (String)sourceCombo_.getSelectedItem();
        srcFilter = new PertFilter(PertFilter.SOURCE, PertFilter.STR_EQUALS, useSrc);
      }
      
      PertFilter targFilter;
      if (targCombo_.getSelectedIndex() == 0) {
        targFilter = null;
      } else {
        String useTarg = (String)targCombo_.getSelectedItem();
        targFilter = new PertFilter(PertFilter.TARGET, PertFilter.STR_EQUALS, useTarg);
      }
      
      if ((srcFilter == null) && (targFilter == null)) {
        pertFilterExpr_ = new PertFilterExpression(PertFilterExpression.ALWAYS_OP);
      } else if (srcFilter != null) {
        pertFilterExpr_ = new PertFilterExpression(srcFilter);
        if (targFilter != null) {
          pertFilterExpr_ = new PertFilterExpression(PertFilterExpression.AND_OP, pertFilterExpr_, targFilter);
        }
      } else { // if (targFilter != null)
        pertFilterExpr_ = new PertFilterExpression(targFilter);       
      }
      haveResult_ = true;
      return (true);
    } else {
      haveResult_ = false;
      return (true);
    }
  }
}
