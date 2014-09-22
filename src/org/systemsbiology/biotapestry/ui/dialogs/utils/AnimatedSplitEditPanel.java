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

package org.systemsbiology.biotapestry.ui.dialogs.utils;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Panel for creating or editing a perturbation measurement type
*/

public abstract class AnimatedSplitEditPanel extends JPanel implements DialogSupport.DialogSupportClient {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  public final static int NO_MODE    = 0;
  public final static int EDIT_MODE  = 1;
  public final static int MERGE_MODE = 2; 
  public final static int DUP_MODE   = 3;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  protected final static String EDIT_CARD  = "edit";
  protected final static String MERGE_CARD  = "merge";
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  protected PendingEditTracker pet_;
  protected JFrame parent_;
  protected BTState appState_;
  protected DataAccessContext dacx_;
  protected String myKey_;
  protected ResourceManager rMan_;    
  protected GridBagConstraints gbc_;
  protected DialogSupport ds_;
  protected int rowNum_;
  protected int colNum_;
  protected boolean editInProgress_;
  protected int mode_;
  protected CardLayout cardLayout_;
  protected JPanel myCard_;
  
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
  
  public AnimatedSplitEditPanel(BTState appState, DataAccessContext dacx, JFrame parent, 
                                PendingEditTracker pet, String myKey, int colNum) { 
    appState_ = appState;
    dacx_ = dacx;
    parent_ = parent;
    pet_ = pet;
    myKey_ = myKey;
    colNum_ = colNum;
    editInProgress_ = false;
    mode_ = NO_MODE;
    
    rMan_ = appState_.getRMan();
    setLayout(new GridBagLayout());
    gbc_ = new GridBagConstraints();
    ds_ = new DialogSupport(this, appState_, gbc_);
    rowNum_ = 0;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  /***************************************************************************
  **
  ** Get the current mode
  ** 
  */
  
  public int getMode() {
    return (mode_);
  }
    
  /***************************************************************************
  **
  ** Answer if we are active
  ** 
  */
 
  public boolean editInProgress() {
    return (editInProgress_);
  }
  
  /***************************************************************************
  **
  ** Tell us we are pushed.  In this case, the pusher will handle the operation,
  ** so we do NOT call our pet!
  ** 
  */
 
  public void startAPush() {
    editInProgress_ = true;
    return;
  }
   
  /***************************************************************************
  **
  ** Trigger the editing
  ** 
  */
 
  public void startEditing() {
    editInProgress_ = true;
    pet_.editIsPending(myKey_);
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
    if (stashResults()) {
      editInProgress_ = false;
      if (mode_ == NO_MODE) {
        throw new IllegalStateException();
      }
      pet_.editIsComplete(myKey_, mode_);
    }
    return;   
  }
   
  /***************************************************************************
  **
  ** Standard close
  ** 
  */
  
  public void closeAction() {
    editInProgress_ = false;
    pet_.editIsCancelled(myKey_);
    return;
  }
 
  /***************************************************************************
  **
  ** Do a hot update of the state
  ** 
  */
  
  public void hotUpdate(boolean mustDie) {
    if (!editInProgress_) {
      return;
    }
    FreezeDried fd = getFreezeDriedState();
    updateOptions();
    if (mustDie || fd.needToCancel()) {
      return;
    }
    fd.reInstall();
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Make this multi modal
  */ 
  
  protected void makeMultiMode(JPanel editPanel, JPanel mergePanel, int rowHeight) {    
    myCard_ = new JPanel();
    cardLayout_ = new CardLayout();
    myCard_.setLayout(cardLayout_); 
    myCard_.add(editPanel, EDIT_CARD);
    myCard_.add(mergePanel, MERGE_CARD);   
    UiUtil.gbcSet(gbc_, 0, rowNum_, colNum_, rowHeight, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.CEN, 1.0, 1.0);
    add(myCard_, gbc_);
    rowNum_ += rowHeight;
    return;
  }
 
  /***************************************************************************
  **
  ** Finish building
  */ 
  
  protected void finishConstruction() {     
    ds_.buildAndInstallCenteredButtonBox(this, rowNum_, colNum_, false, true); 
    return;
  }
  
  /***************************************************************************
  **
  ** Update the options
  */ 
  
  protected abstract void updateOptions();   
  
  /***************************************************************************
  **
  ** Get current state
  */ 
  
  protected abstract FreezeDried getFreezeDriedState(); 
 
  /***************************************************************************
  **
  ** Install our UI values:
  ** 
  */
  
  protected abstract boolean stashResults();
  
    ////////////////////////////////////////////////////////////////////////////
  //
  // INNER CLASSES
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Freeze dried state for hot updates
  */

  public interface FreezeDried { 
    public boolean needToCancel(); 
    public void reInstall();
  }
}