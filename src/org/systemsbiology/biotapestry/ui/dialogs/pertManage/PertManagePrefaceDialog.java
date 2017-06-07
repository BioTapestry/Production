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

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.PertFilter;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TrueObjChoiceContent;
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
  
  public PertManagePrefaceDialog(UIComponentSource uics, DataAccessContext dacx, JFrame parent) {     
    super(parent, dacx.getRMan().getString("pertManagePreface.title"), true);
    uics_ = uics;
    dacx_ = dacx;
    haveResult_ = false;
    
    ResourceManager rMan = dacx.getRMan();    
    setSize(600, 350);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();  
    
    JLabel mainLabel = new JLabel(rMan.getString("pertManagePreface.chooseOpFilters"));
    int rowNum = 0;
    UiUtil.gbcSet(gbc, 0, rowNum++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    cp.add(mainLabel, gbc);
     
    PerturbationData pd = dacx_.getExpDataSrc().getPertData();
    
    SortedSet<TrueObjChoiceContent> srcCand = pd.getCandidates(PertFilter.Cat.SOURCE);
    TrueObjChoiceContent ncstr = new TrueObjChoiceContent(rMan.getString("pertManagePreface.chooseAll"), null);
    Vector<TrueObjChoiceContent> srcVec = new Vector<TrueObjChoiceContent>();
    srcVec.add(ncstr);
    srcVec.addAll(srcCand);
    
    JLabel srcLabel = new JLabel(rMan.getString("pertManagePreface.chooseSource"));
    sourceCombo_ = new JComboBox(srcVec);
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    cp.add(srcLabel, gbc);
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(sourceCombo_, gbc);    
  
    SortedSet<TrueObjChoiceContent> targCand = pd.getCandidates(PertFilter.Cat.TARGET);
    Vector<TrueObjChoiceContent> targVec = new Vector<TrueObjChoiceContent>();
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
          uics_.getExceptionHandler().displayException(ex);
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
        TrueObjChoiceContent useSrc = (TrueObjChoiceContent)sourceCombo_.getSelectedItem();
        srcFilter = new PertFilter(PertFilter.Cat.SOURCE, PertFilter.Match.STR_EQUALS, useSrc.val);
      }
      
      PertFilter targFilter;
      if (targCombo_.getSelectedIndex() == 0) {
        targFilter = null;
      } else {
        TrueObjChoiceContent useTarg = (TrueObjChoiceContent)targCombo_.getSelectedItem();
        targFilter = new PertFilter(PertFilter.Cat.TARGET, PertFilter.Match.STR_EQUALS, useTarg.val);
      }
      
      if ((srcFilter == null) && (targFilter == null)) {
        pertFilterExpr_ = new PertFilterExpression(PertFilterExpression.Op.ALWAYS_OP);
      } else if (srcFilter != null) {
        pertFilterExpr_ = new PertFilterExpression(srcFilter);
        if (targFilter != null) {
          pertFilterExpr_ = new PertFilterExpression(PertFilterExpression.Op.AND_OP, pertFilterExpr_, targFilter);
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
