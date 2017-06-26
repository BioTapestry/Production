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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.systemsbiology.biotapestry.app.UIComponentSource;

/****************************************************************************
**
** A helper for Stash Input panels
*/

public class BTStashInputPanel extends JPanel implements DialogSupport.DialogSupportClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private BTStashResultsDialog owner_;
  protected DialogSupport ds_;
  protected GridBagConstraints gbc_;
  protected int rowNum_;
  protected JTextField textField_;
  protected String answer_;
   
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
  
  public BTStashInputPanel(BTStashResultsDialog owner, UIComponentSource uics, String question, String title) {     
    owner_ = owner;
    answer_ = null;
    gbc_ = new GridBagConstraints();
    rowNum_ = 0;
    ds_ = new DialogSupport(this, uics, gbc_);
    setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED), new TitledBorder(new LineBorder(Color.BLACK, 3), ds_.getRman().getString(title), TitledBorder.CENTER, TitledBorder.ABOVE_TOP)));
    setLayout(new GridBagLayout());
    textField_ = new JTextField(20);
    rowNum_ = ds_.installLabeledJComp(textField_, this, question, rowNum_, 2);
    ds_.buildAndInstallCenteredButtonBox(this, rowNum_, 2, false, true);      
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 

  /***************************************************************************
  **
  ** Position myself
  */ 
  
  protected void positionInputPane(JPanel myCard) {
    myCard.setLayout(new BorderLayout());
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue());
    buttonPanel.add(this);
    buttonPanel.add(Box.createHorizontalGlue());
    myCard.add(buttonPanel, BorderLayout.CENTER);
    return;
  }

  /***************************************************************************
  **
  ** Init Text
  */   
  
  protected void initText(String fill) {
    textField_.setText(fill);
    return;
  }

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
    answer_ = textField_.getText().trim();
    if (owner_.processInputAnswer()) {
      owner_.cancelInputPane();
    }
    return;   
  }
   
  /***************************************************************************
  **
  ** Standard close
  ** 
  */
  
  public void closeAction() {
    owner_.cancelInputPane();
    return;
  }
  
  /***************************************************************************
  **
  ** Answer
  ** 
  */
  
  public String getAnswer() {
    return (answer_);
  }
  
  /***************************************************************************
  **
  ** Preferred
  */

  @Override
  public Dimension getPreferredSize() {
    Dimension ps = super.getPreferredSize();
    return (ps);
  }  
 
  /***************************************************************************
  **
  ** Fixed minimum
  */

  @Override
  public Dimension getMinimumSize() {
    return (getPreferredSize());
  }
  
  /***************************************************************************
  **
  ** Fixed Maximum
  */

  @Override
  public Dimension getMaximumSize() {
    return (getPreferredSize());
  } 
}
