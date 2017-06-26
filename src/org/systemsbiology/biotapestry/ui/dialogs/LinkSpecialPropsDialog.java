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
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.db.ColorResolver;
import org.systemsbiology.biotapestry.ui.LinkProperties;
import org.systemsbiology.biotapestry.ui.PerLinkDrawStyle;
import org.systemsbiology.biotapestry.ui.SuggestedDrawStyle;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.ColorDeletionListener;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for editing special drawing properties for entire links (i.e.
** one single path through a link tree)
*/

public class LinkSpecialPropsDialog extends JDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private String treeColor_;

  private PerLinkDrawStyle oldPlds_;
  private PerLinkDrawStyle newPlds_;
  private boolean haveResult_;
   
  private SuggestedDrawStylePanel sdsPan_;
  private JComboBox extentCombo_;
  private ColorResolver cRes_;
  private UIComponentSource uics_;
  
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
  
  public LinkSpecialPropsDialog(UIComponentSource uics, ColorResolver cRes, HarnessBuilder hBld, LinkProperties propsClone, 
                                String linkID, List<ColorDeletionListener> colorDeletionListeners) {
    this(uics, cRes, hBld, true, propsClone.getColorName(), propsClone.getDrawStyleForLinkage(linkID), colorDeletionListeners);
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public LinkSpecialPropsDialog(UIComponentSource uics, ColorResolver cRes, HarnessBuilder hBld, PerLinkDrawStyle oldPlds) {
    this(uics, cRes, hBld, false, null, oldPlds, null);
  }
  
  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  private LinkSpecialPropsDialog(UIComponentSource uics, ColorResolver cRes, HarnessBuilder hBld, boolean allowColor, String oldColor, 
                                 PerLinkDrawStyle oldPlds, List<ColorDeletionListener> colorDeletionListeners) {     
    super(uics.getTopFrame(), uics.getRMan().getString("perLinkSpecial.setSpecial"), true);
    uics_ = uics;
    cRes_ = cRes;
    treeColor_ = oldColor;
    oldPlds_ = oldPlds;
    haveResult_ = false;

    ResourceManager rMan = uics_.getRMan();
    setSize(600, 350);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    if (allowColor) {
      sdsPan_ = new SuggestedDrawStylePanel(uics_, cRes_, hBld, true, colorDeletionListeners); 
    } else {
      sdsPan_ = new SuggestedDrawStylePanel(uics_, cRes_, hBld, true);    
    }
    UiUtil.gbcSet(gbc, 0, 0, 10, 8, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);    
    cp.add(sdsPan_, gbc);
    
    JLabel comboLabel = new JLabel(rMan.getString("perLinkSpecial.extent"));
    Vector<ChoiceContent> extentChoices = PerLinkDrawStyle.getExtentChoices(uics_);
    extentCombo_ = new JComboBox(extentChoices);
    
    UiUtil.gbcSet(gbc, 0, 10, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(comboLabel, gbc);
    UiUtil.gbcSet(gbc, 1, 10, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(extentCombo_, gbc);
    
    //
    // Build the button panel:
    //

    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            LinkSpecialPropsDialog.this.setVisible(false);
            LinkSpecialPropsDialog.this.dispose();
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          stashResults(false);
          LinkSpecialPropsDialog.this.setVisible(false);
          LinkSpecialPropsDialog.this.dispose();
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
    UiUtil.gbcSet(gbc, 0, 11, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
    displayProperties();
  }
  
  /***************************************************************************
  **
  ** Get the new draw style.  May be null
  ** 
  */
  
  public PerLinkDrawStyle getProps() {
    return (newPlds_);
  }
  
  /***************************************************************************
  **
  ** Answer if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
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
  ** Apply the current data values to our UI components
  ** 
  */
  
  private void displayProperties() {
    //
    // Want to set these settings to current default values applied to whole
    // tree (or maybe even to link-specific)?
    //    
    SuggestedDrawStyle oldSds = (oldPlds_ == null) ? null : oldPlds_.getDrawStyle();    
    sdsPan_.displayProperties(oldSds, new SuggestedDrawStyle(treeColor_));
    
    int showExtent = (oldPlds_ == null) ? PerLinkDrawStyle.UNIQUE : oldPlds_.getExtent();
    extentCombo_.setSelectedItem(PerLinkDrawStyle.extentForCombo(uics_, showExtent));

    return;
  }
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation.  If they have an error, pop
  ** up a warning dialog and return false, else return true.
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      SuggestedDrawStylePanel.QualifiedSDS qsds = sdsPan_.getChosenStyle();
      if (!qsds.isOK) {
        return (false);
      }
      
      if (qsds.sds == null) {
        newPlds_ = null;
        haveResult_ = true;
        return (true);
      }
      
      int extentVal =  ((ChoiceContent)extentCombo_.getSelectedItem()).val;      
     
      newPlds_ = new PerLinkDrawStyle(qsds.sds, extentVal);
      
      //
      // FIX ME!  Compare to other per-link values here!
      //
 
      /*
      if (props_.linkPropsInconsistent(newPlds_)) {
        ResourceManager rMan = ResourceManager.getManager(); 
        JOptionPane.showMessageDialog(this, rMan.getString("dicreate.badNumber"),
                                      rMan.getString("dicreate.badNumberTitle"), 
                                      JOptionPane.ERROR_MESSAGE);
        haveResult_ = false;
        return (false);
      }
      */
      
      haveResult_ = true;
      return (true);
    } else {
      newPlds_ = null;
      haveResult_ = false;
      return (true);
    }
  }
}
