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

import java.awt.CardLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.util.AnimatedSplitPane;
import org.systemsbiology.biotapestry.util.AnimatedSplitPaneLayoutManager;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;

/****************************************************************************
**
** Panel for bouncing edits
*/

public abstract class AnimatedSplitManagePanel extends JPanel implements PendingEditTracker, 
                                                                         AnimatedSplitPane.AnimatedSplitListener,
                                                                         SelectionOracle {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  //////////////////////////////////////////////////////////////////////////// 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  protected AnimatedSplitPaneLayoutManager.PanelForSplit topPanel_;
  protected JFrame parent_;
  protected String myKey_;
  private AnimatedSplitPaneLayoutManager.PanelForSplit editPanel_;
  private CardLayout cardLayout_;
  private AnimatedSplitPane asp_;
  protected PendingEditTracker pet_;
  protected GridBagConstraints gbc_; 
  protected int rowNum_;
  protected ResourceManager rMan_;
  private int currEditDepth_;
  private String[] editStack_;
  protected boolean editInProgress_;
  protected UIComponentSource uics_;
  protected DataAccessContext dacx_;
  
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
  
  public AnimatedSplitManagePanel(UIComponentSource uics, DataAccessContext dacx, JFrame parent, PendingEditTracker pet, String key) {
    uics_ = uics;
    dacx_ = dacx;
    pet_ = pet;
    myKey_ = key;
    parent_ = parent;
    currEditDepth_ = 0;
    editStack_ = null;
    setLayout(new GridLayout(1, 1));
    rMan_ = dacx_.getRMan();
       
    topPanel_ = new AnimatedSplitPaneLayoutManager.PanelForSplit();
    topPanel_.setLayout(new GridBagLayout());
    gbc_ = new GridBagConstraints();
    rowNum_ = 0;
      
    editPanel_ = new AnimatedSplitPaneLayoutManager.PanelForSplit();
    cardLayout_ = new CardLayout();
    editPanel_.setLayout(cardLayout_); 
    editPanel_.add(new JPanel(), "blank");
    editInProgress_ = false;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** For table row support
  */   
  
  public abstract boolean tableRowMatches(String key, Object obj, String tableID);
   
  /***************************************************************************
  **
  ** Get the tag
  ** 
  */
  
  public String getTag() {
    return (myKey_);
  }
 
  /***************************************************************************
  **
  ** Tell if we are editing
  ** 
  */
  
  public boolean havePendingEdit() {
    return (editInProgress_);
  }
  
  /***************************************************************************
  **
  ** Let us know if we have a pending edit.
  ** 
  */
  
  public void editIsPending(String key) {
    enableTopPane(false);
    pet_.editIsPending(myKey_);
    cardLayout_.show(editPanel_, key);
    editInProgress_ = true;
    asp_.expand();
    return;
  }
  
  /***************************************************************************
  **
  ** Let us know if we have a completed edit.
  ** 
  */
  
  public void editIsComplete(String key, int what) {
    if (currEditDepth_ != 0) {
      currEditDepth_--;
      asp_.collapseThenExpand();
    } else {
      displayProperties(true);
      enableTopPane(true); 
      pet_.editIsComplete(myKey_, 0);
      asp_.collapse();
      cardLayout_.show(editPanel_, "blank");
      editInProgress_ = false;
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Let us know if we have a cancelled edit.
  ** 
  */
  
  public void editIsCancelled(String key) {
    if (currEditDepth_ != 0) {
      currEditDepth_--;
      asp_.collapseThenExpand();
    } else {
      enableTopPane(true);
      pet_.editIsCancelled(myKey_);
      asp_.collapse();
      cardLayout_.show(editPanel_, "blank");
      editInProgress_ = false;
    }
    return;
  }

  /***************************************************************************
  **
  ** Let us know if we have a pushed edit
  ** 
  */
  
  public void editIsPushed(String key) {
    currEditDepth_++;
    asp_.collapseThenExpand();
    return;
  }
    
  /***************************************************************************
  **
  ** Let us know if we have deletion.
  ** 
  */
  
  public void itemDeleted(String key) {
    // Override for interesting behavior...
    return;
  }
      
  /***************************************************************************
  **
  ** Let us know before edit is submitted
  ** 
  */
 
  public void editSubmissionBegins(){
    // Override for interesting behavior...
    return;
  }
  
  /***************************************************************************
  **
  ** Let us know after edit submission is done
  ** 
  */ 
  
  public void editSubmissionEnds(){
    // Override for interesting behavior...
    return;
  }
 
  /***************************************************************************
  **
  ** Let us know if we need to jump to another editor
  ** 
  */
  
  public void jumpToRemoteEdit(String key, String tableTarg, String rowTarg) {
    pet_.jumpToRemoteEdit(key, tableTarg, rowTarg);
    return;
  }
  
  /***************************************************************************
  **
  ** Let us know if we are mid bounce, used in push/pop cases:
  ** 
  */
  
  public void midBounce() {
    if (editStack_ == null) {
      return;
    }     
    String targetKey = editStack_[currEditDepth_];
    cardLayout_.show(editPanel_, targetKey);
    return;
  }
  
  /***************************************************************************
  **
  **  Override to get this info on the editor expanding or contracting
  */ 
  
  public void finished() {  
    return;
  } 
  
  /***************************************************************************
  **
  ** Get the split for repair needs
  */ 
  
  public AnimatedSplitPane getSplit() {  
    return (asp_);
  } 
  
  /***************************************************************************
  **
  ** Tell panel we have a change
  */ 
  
  public abstract void haveAChange(boolean mustDie);
  
  /***************************************************************************
  **
  ** make a table selection
  */ 
  
  public abstract void setTableSelection(String whichTable, String whichKey);
 
  /***************************************************************************
  **
  ** Tell panel we have a change
  */ 
  
  public abstract void dropPendingEdits();
   
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PROTECTED METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Common table build code
  */ 
  
  protected JPanel commonTableBuild(ReadOnlyTable rot, String title, 
                                    ReadOnlyTable.ButtonHandler bh, 
                                    List<ReadOnlyTable> allTabs, List<ReadOnlyTable.ColumnWidths> colWidths, String key) {    
    JPanel display = new JPanel();
    display.setBorder(BorderFactory.createEtchedBorder());
    display.setLayout(new GridBagLayout());
    rot.setButtonHandler(bh);
    ReadOnlyTable.TableParams tp = new ReadOnlyTable.TableParams();
    tp.disableColumnSort = false;
    tp.tableIsUnselectable = false;
    tp.buttons = ReadOnlyTable.ALL_BUTTONS;
    if (allTabs == null) {
      tp.multiTableSelectionSyncing = null;
    } else {
      tp.multiTableSelectionSyncing = allTabs;
      tp.clearOthersOnSelect = true; 
    }
    tp.buttonsOnSide = true;
    tp.tableTitle = rMan_.getString(title);
    tp.titleFont = null;
    tp.colWidths = colWidths;
    tp.canMultiSelect = true;
    if (key != null) {
      addExtraButtons(tp, key);
    }
    JPanel tabPan = rot.buildReadOnlyTable(tp);
    UiUtil.gbcSet(gbc_, 0, 0, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    display.add(tabPan, gbc_);
    return (display);  
  }
  
  /***************************************************************************
  **
  ** Add extra buttons
  ** 
  */
  
  public void addExtraButtons(ReadOnlyTable.TableParams etp, String targKeyArg) {
    if (etp.userAddedButtons == null) {
      etp.userAddedButtons = new ArrayList<ReadOnlyTable.ExtraButton>();
    }
    String buttonTag = rMan_.getString("asmp.mergeSelected");
    JButton myButton = (etp.buttonsOnSide) ? new FixedJButton(FixedJButton.VERT_ONLY_FIXED, buttonTag) : new FixedJButton(buttonTag);
    final String targKey = targKeyArg;
    myButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {   
          doAJoin(targKey);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    ReadOnlyTable.ExtraButton eb = new ReadOnlyTable.ExtraButton(false, false, true, myButton);
    etp.userAddedButtons.add(eb); 
    
    buttonTag = rMan_.getString("asmp.dupSelected");
    myButton = (etp.buttonsOnSide) ? new FixedJButton(FixedJButton.VERT_ONLY_FIXED, buttonTag) : new FixedJButton(buttonTag);
    myButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {   
          doADuplication(targKey);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    eb = new ReadOnlyTable.ExtraButton(false, true, false, myButton);
    etp.userAddedButtons.add(eb); 
    
    buttonTag = rMan_.getString("asmp.jumpAndFilter");
    myButton = (etp.buttonsOnSide) ? new FixedJButton(FixedJButton.VERT_ONLY_FIXED, buttonTag) : new FixedJButton(buttonTag);
    myButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {   
          doAFilterJump(targKey);
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    eb = new ReadOnlyTable.ExtraButton(false, true, false, myButton);
    etp.userAddedButtons.add(eb); 
    
    return;
  }
 
  /***************************************************************************
  **
  ** Add an edit panel
  */ 
  
  protected void addEditPanel(JPanel who, String id) {
    editPanel_.add(who, id);
    return;
  }

  /***************************************************************************
  **
  ** To edit stack construction
  */ 
  
  protected void setEditStack(String[] stack) {
    editStack_ = stack;
    return;
  }

  /***************************************************************************
  **
  ** Finish the building
  */ 
  
  protected void finishConstruction() {
    asp_ = new AnimatedSplitPane(uics_, dacx_, topPanel_, editPanel_, this);
    add(asp_);
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // ABSTRACT METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Handle top pane enabling
  */ 
  
  protected abstract void enableTopPane(boolean enable);
    
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  protected abstract void displayProperties(boolean fireChange);
  
  /***************************************************************************
  **
  ** Handle add operations 
  */ 
  
  protected abstract void doAnAdd(String key);
   
  /***************************************************************************
  **
  ** Handle edit operations 
  */ 
  
  protected abstract void doAnEdit(String key);
 
  
  /***************************************************************************
  **
  ** Handle delete operations 
  */ 
  
  protected abstract void doADelete(String key);
  
  /***************************************************************************
  **
  ** Handle merging operations 
  */ 
  
  public void doAJoin(String key) {
    return;
  }
  
  /***************************************************************************
  **
  ** Handle duplication operations 
  */ 
  
  public void doADuplication(String key) {
    return;
  }
  
  /***************************************************************************
  **
  ** Handle filter jumps
  */ 
  
  public void doAFilterJump(String key) {
    return;
  }  
    
  /***************************************************************************
  **
  ** Used for tracking button presses
  */

  public class ButtonHand implements ReadOnlyTable.ButtonHandler {
    
    private String key_;
    
    public ButtonHand(String key) {
      key_ = key;
    }
    
    public void pressed(int whichButton) {
      switch (whichButton) {
        case ReadOnlyTable.ADD_BUTTON:
          doAnAdd(key_);
          break;
        case ReadOnlyTable.DELETE_BUTTON:
          doADelete(key_);
          break;
        case ReadOnlyTable.EDIT_BUTTON:
          doAnEdit(key_);
          break;
        default:
          throw new IllegalArgumentException();
      }
      return;
    }
  } 
}
