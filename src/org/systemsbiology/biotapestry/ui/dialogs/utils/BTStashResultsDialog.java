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


package org.systemsbiology.biotapestry.ui.dialogs.utils;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.ResourceManager;

/****************************************************************************
**
** Abstract base class for stash results dialogs
*/

public abstract class BTStashResultsDialog extends JDialog implements DialogSupport.DialogSupportClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private boolean haveResult_;
  protected JPanel cp_;
  protected JPanel truPane_;
  protected JPanel backPane_;
  protected ResourceManager rMan_;
  protected UIComponentSource uics_;
  protected DialogSupport ds_;
  protected GridBagConstraints gbc_;
  protected int rowNum_;
  protected int columns_;
  protected CardLayout myCard_;
  protected BTStashInputPanel ip_;
  protected boolean showingPrime_;
  protected DataAccessContext dacx_;
   
  private static final long serialVersionUID = 1L;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  protected BTStashResultsDialog(UIComponentSource uics, DataAccessContext dacx, String titleResource, Dimension size, int columns) {     
    super(uics.getTopFrame(), dacx.getRMan().getString(titleResource), true);
    haveResult_ = false;
    uics_ = uics;
    dacx_ = dacx;
    rMan_ = dacx_.getRMan();    
    setSize(size.width, size.height);
    truPane_ = (JPanel)getContentPane();
    myCard_ = new CardLayout();
    truPane_.setLayout(myCard_);
    backPane_ = new JPanel();
    backPane_.setBackground(new Color(160, 160, 160));
    truPane_.add(backPane_, "Backup");   
    cp_ = new JPanel();
    truPane_.add(cp_, "Prime");
    myCard_.show(truPane_, "Prime");
    showingPrime_ = true;
    cp_.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp_.setLayout(new GridBagLayout());
    gbc_ = new GridBagConstraints();
    rowNum_ = 0;
    ds_ = new DialogSupport(this, uics, dacx, gbc_);
    columns_ = columns;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Standard apply
  ** 
  */
 
  public void applyAction() { 
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  **
  ** Standard ok
  ** 
  */
 
  public void okAction() {
    if (stashResults(true)) {
      setVisible(false);
      dispose();
    }
    return;   
  }
   
  /***************************************************************************
  **
  ** Standard close
  ** 
  */
  
  public void closeAction() {
    if (stashResults(false)) {
      setVisible(false);
      dispose();
    }
    return;
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
  ** Do handling of the inputPane. Override if using input pane function.
  */
  
  public boolean processInputAnswer() {
    throw new UnsupportedOperationException();
  }
  
  /***************************************************************************
  **
  ** Cancel the overpane
  ** 
  */
 
  public void cancelInputPane() { 
    myCard_.show(truPane_, "Prime");
    showingPrime_ = true;
    truPane_.validate();
    return;
  } 

  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    
  
  /***************************************************************************
  **
  ** Show Overpane
  */ 
  
  protected void buildInputPane(String question, String title) {
    ip_ = new BTStashInputPanel(this, uics_, dacx_, question, title);
    ip_.positionInputPane(backPane_);
    return;
  }
  
  /***************************************************************************
  **
  ** Show Overpane
  */ 
  
  protected void buildExtraPane(JPanel panel) {
    backPane_.setLayout(new BorderLayout());
    backPane_.add(panel, BorderLayout.CENTER);
    return;
  }

  /***************************************************************************
  **
  ** Init Text
  */   
  
  protected void initInputPane(String fill) {
    ip_.initText(fill);
    return;
  }
 
  /***************************************************************************
  **
  ** Show Overpane
  */ 
  
  protected void showInputPane(boolean doit) {
    myCard_.show(truPane_, (doit) ? "Backup" : "Prime");
    showingPrime_ = !doit;
    truPane_.validate();
    return;
  } 

  /***************************************************************************
  **
  ** Answer if showing input panel
  */ 
  
  protected boolean showingInputPane() {
    return (!showingPrime_);
  }

  /***************************************************************************
  **
  ** Get answer
  ** 
  */
  
  public String getInputAnswer() {
    return (ip_.getAnswer());
  }

  /***************************************************************************
  **
  ** Add a full row component
  */ 
  
  protected void addWidgetFullRow(JComponent comp, boolean fixHeight) {
    addWidgetFullRow(comp, fixHeight, false);
    return;
  } 
  
  /***************************************************************************
  **
  ** Add a full row component
  */ 
  
  protected void addWidgetFullRow(JComponent comp, boolean fixHeight, boolean flushLeft) {
    rowNum_ = ds_.addWidgetFullRow(cp_, comp, fixHeight, flushLeft, rowNum_, columns_);
    return;
  } 
  
  /***************************************************************************
  **
  ** Add a full row component
  */ 
  
  protected void addTallWidgetFullRow(JComponent comp, boolean fixHeight, boolean flushLeft, int height) {
    rowNum_ = ds_.addTallWidgetFullRow(cp_, comp, fixHeight, flushLeft, height, rowNum_, columns_); 
    return;
  } 

   /***************************************************************************
  **
  ** Add a full row component
  */ 
  
  protected void addWidgetFullRowWithInsets(JComponent comp, boolean fixHeight, int inst, int insl, int insb, int insr) {
    rowNum_ = ds_.addWidgetFullRowWithInsets(cp_, comp, fixHeight, inst, insl, insb, insr, rowNum_, columns_);
    return;
  } 

  /***************************************************************************
  **
  ** Add a full row component with a label
  */ 
  
  protected void addLabeledWidget(JLabel label, JComponent comp, boolean fixHeight, boolean flushLeft) {
    rowNum_ = ds_.addLabeledWidget(cp_, label, comp, fixHeight, flushLeft, rowNum_, columns_);
    return;
  }
 
  /***************************************************************************
  **
  ** Add a component with a label, no stretch
  */ 
  
  protected void addLabeledWidgetNoStretch(JLabel label, JComponent comp, boolean fixHeight, boolean flushLeft) {
    rowNum_ = ds_.addLabeledWidgetNoStretch(cp_, label, comp, fixHeight, flushLeft, rowNum_, columns_);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a labeled scrolled component
  */ 
   
  protected void addLabeledScrolledWidget(String labelKey, JComponent comp, int scrollRows) {
    rowNum_ = ds_.addLabeledScrolledWidget(cp_, labelKey, comp, rowNum_, columns_, scrollRows);
    return;
  } 
  
  /***************************************************************************
  **
  ** Add a full row component with a label and insets
  */ 
  
  protected void addLabeledWidgetWithInsets(JLabel label, JComponent comp, boolean fixHeight, boolean flushLeft, 
                                            int inst, int insl, int insb, int insr) {
    rowNum_ = ds_.addLabeledWidgetWithInsets(cp_, label, comp, fixHeight, flushLeft, inst, insl, insb, insr, rowNum_, columns_);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a labeled button pair to a full row
  */ 
   
  protected void addLabeledButtonPair(JLabel label, JRadioButton but1, JRadioButton but2) {
    rowNum_ = ds_.addLabeledButtonPair(cp_, label, but1, but2, rowNum_, columns_);
    return;
  }
  
  /***************************************************************************
  **
  ** Add two (really) labeled components to one row
  */ 
     
  protected void addTwoTrueLabeledWidgets(JLabel label1, JComponent comp, JLabel label2, JComponent comp2, boolean gottaGrow) {
    rowNum_ = ds_.installTrueLabelJCompPair(cp_, label1, comp, label2, comp2, rowNum_,columns_, gottaGrow);
    return;
  } 
  
  /***************************************************************************
  **
  ** Add a table
  */ 
  
  protected void addTable(JComponent tablePan, int rowHeight) { 
    rowNum_ = ds_.addTable(cp_, tablePan, rowHeight, rowNum_, columns_);
    return;
  }
  
  /***************************************************************************
  **
  ** Add a table
  */ 
  
  protected void addTableNoInset(JComponent tablePan, int rowHeight) {
    rowNum_ = ds_.addTableNoInset(cp_, tablePan, rowHeight, rowNum_, columns_);
    return;
  } 
  
  /***************************************************************************
  **
  ** Add a row of labeled widgets
  */ 
  
  public void addSeriesToRow(List<JLabel> labs, List<JComponent> comps, int start) { 
    rowNum_ = ds_.addSeriesToRow(cp_, labs, comps, rowNum_ ,start);
    return;
  }
    
  /***************************************************************************
  **
  ** Finish building
  */ 
  
  protected void finishConstruction() {      
    ds_.buildAndInstallButtonBox(cp_, rowNum_, columns_, false, true);
    setLocationRelativeTo(uics_.getTopFrame());
    return;
  } 
  
  /***************************************************************************
  **
  ** Finish building
  */ 
  
  protected void finishConstructionForApply() {
    ds_.buildAndInstallButtonBox(cp_, rowNum_, columns_, true, false);
    setLocationRelativeTo(uics_.getTopFrame());
    return;
  } 
  
  /***************************************************************************
  **
  ** Finish building
  */ 
  
  protected void finishConstructionWithExtraLeftButton(JButton xtraButton) { 
    ds_.buildAndInstallButtonBoxWithExtra(cp_, rowNum_, columns_, false, xtraButton, true);    
    setLocationRelativeTo(uics_.getTopFrame());
    return;
  }   
  
  /***************************************************************************
  **
  ** Finish building
  */ 
  
  protected void finishConstructionWithMultiExtraLeftButtons(List<JButton> xtraButtonList) { 
    ds_.buildAndInstallButtonBoxWithMultiExtra(cp_, rowNum_, columns_, false, xtraButtonList, true);    
    setLocationRelativeTo(uics_.getTopFrame());
    return;
  }   
   
  /***************************************************************************
  **
  ** Stash our results for later interrogation. 
  ** 
  */
  
  protected boolean stashResults(boolean ok) {
    if (ok) {
      if (stashForOK()) {
        haveResult_ = true;
        return (true);
      } else {
        haveResult_ = false;
        return (false);
      }
    } else {
      haveResult_ = false;
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Do the stashing 
  ** 
  */
  
  protected abstract boolean stashForOK();

}
