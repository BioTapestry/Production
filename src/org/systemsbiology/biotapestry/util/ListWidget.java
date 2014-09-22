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

package org.systemsbiology.biotapestry.util;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/****************************************************************************
**
** A list widget
*/

public class ListWidget extends JPanel implements ListSelectionListener {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  public final static int ADD_ONLY       = 0;
  public final static int DELETE_ONLY    = 1;  
  public final static int FULL_MODE      = 2;
  public final static int DELETE_COMBINE = 3;
  public final static int DELETE_EDIT    = 4;  
  public final static int ADD_ONLY_HOLD_SELECT = 5;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VISIBLE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private JButton buttonA_;
  private JButton buttonD_;
  private JButton buttonE_;
  private JButton buttonC_;
  private JButton buttonExtra_;
  private JList list_;
  private int[] selectedRows_;
  private ListWidgetClient client_;
  private int mode_;
  private HandlerAndManagerSource hams_;
  
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

  public ListWidget(HandlerAndManagerSource hams, List contents, ListWidgetClient client) {
    this(hams, contents, client, false);
  }

  /***************************************************************************
  **
  ** Constructor
  */

  public ListWidget(HandlerAndManagerSource hams, List contents, ListWidgetClient client, boolean addOnly) {
    this(hams, contents, client, (addOnly) ? ADD_ONLY : FULL_MODE, null);
  }  
  
  /***************************************************************************
  **
  ** Constructor
  */

  public ListWidget(HandlerAndManagerSource hams, List contents, ListWidgetClient client, int mode) {
    this(hams, contents, client, mode, null);
  }   
  
  /***************************************************************************
  **
  ** Constructor
  */

  public ListWidget(HandlerAndManagerSource hams, List contents, ListWidgetClient client, int mode, JButton buttonExtra) {
    hams_ = hams;
    ResourceManager rMan = hams_.getRMan();    
    GridBagConstraints gbc = new GridBagConstraints();
    client_ = client;
    mode_ = mode;
    selectedRows_ = new int[0];
    buttonExtra_ = buttonExtra;
    
    setLayout(new GridBagLayout());
    list_ = new JList(contents.toArray());
    list_.addListSelectionListener(this);
 
    if ((mode == ADD_ONLY) || (mode == ADD_ONLY_HOLD_SELECT) || (mode == FULL_MODE)) {
      buttonA_ = new JButton(rMan.getString("dialogs.add"));
      buttonA_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (addRow()) {
              list_.revalidate();
            }
          } catch (Exception ex) {
            hams_.getExceptionHandler().displayException(ex);
          }
        }
      });
    }

    if ((mode == DELETE_ONLY) || 
        (mode == DELETE_COMBINE) ||
        (mode == DELETE_EDIT) ||        
        (mode == FULL_MODE)) {
      buttonD_ = new JButton(rMan.getString("dialogs.delete"));
      buttonD_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (deleteRows(selectedRows_)) {
              list_.revalidate();
            }
          } catch (Exception ex) {
            hams_.getExceptionHandler().displayException(ex);
          }
        }
      });
      buttonD_.setEnabled(false);
    }
    
    if ((mode == FULL_MODE) || (mode == DELETE_EDIT)) {
      buttonE_ = new JButton(rMan.getString("dialogs.edit"));
      buttonE_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (editRow(selectedRows_)) {
              list_.revalidate();
            }
          } catch (Exception ex) {
            hams_.getExceptionHandler().displayException(ex);
          }
        }
      });
      buttonE_.setEnabled(false);
    }
    
    if (mode == DELETE_COMBINE) {
      buttonC_ = new JButton(rMan.getString("dialogs.combine"));
      buttonC_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            if (combineRows(selectedRows_)) {
              list_.revalidate();
            }
          } catch (Exception ex) {
            hams_.getExceptionHandler().displayException(ex);
          }
        }
      });
      buttonC_.setEnabled(false);
    }    

    //
    // Add the add/delete buttons
    //

    UiUtil.gbcSet(gbc, 0, 0, 2, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    JScrollPane jsp = new JScrollPane(list_);
    add(jsp, gbc);
    int rowNum = 0;
    
    if ((mode == ADD_ONLY) || (mode == ADD_ONLY_HOLD_SELECT) || (mode == FULL_MODE)) {
      UiUtil.gbcSet(gbc, 2, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
      add(buttonA_, gbc);
    }
    
    if ((mode == DELETE_ONLY) || 
        (mode == DELETE_COMBINE) || 
        (mode == DELETE_EDIT) ||         
        (mode == FULL_MODE)) {
      UiUtil.gbcSet(gbc, 2, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
      add(buttonD_, gbc);
    }
    
    if ((mode == FULL_MODE) || (mode == DELETE_EDIT)) {    
      UiUtil.gbcSet(gbc, 2, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
      add(buttonE_, gbc);
    }
    
    if (mode == DELETE_COMBINE) {    
      UiUtil.gbcSet(gbc, 2, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
      add(buttonC_, gbc);
    } 
    
    if (buttonExtra_ != null) {
      UiUtil.gbcSet(gbc, 2, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.N, 0.0, 0.0);
      add(buttonExtra_, gbc);
      buttonExtra_.setEnabled(false);
    }
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  
  /***************************************************************************
  **
  ** Set the selections
  */
  
  public void setSelectedIndices(int[] indices) {
    list_.setSelectedIndices(indices);
    if (indices.length > 0) {
      list_.ensureIndexIsVisible(indices[0]);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Get a list item
  */
  
  public Object getElementAt(int index) {
    return (list_.getModel().getElementAt(index));
  }
  
  /***************************************************************************
  **
  ** Get the list count
  */
  
  public int getElementCount() {
    return (list_.getModel().getSize());
  }  
  
  /***************************************************************************
  **
  ** Get the selections
  */
  
  public Object[] getSelectedObjects() {
    return (list_.getSelectedValues());
  }   
  
  /***************************************************************************
  **
  ** Get the selections
  */
  
  public int[] getSelections() {
    return (list_.getSelectedIndices());
  } 
  
  /***************************************************************************
  **
  ** Update the list
  */
  
  public void update(List entries) {
    list_.clearSelection();
    list_.setListData(entries.toArray());
    return;
  }
  
  /***************************************************************************
  **
  ** Add a row to the list
  */
  
  public boolean addRow() {
    List entries = client_.addRow(this);
    if (entries == null) {
      return (false);
    }
    int[] holdRows = null;
    if (mode_ == ADD_ONLY_HOLD_SELECT) {
      holdRows = list_.getSelectedIndices();
    }
    list_.clearSelection();
    list_.setListData(entries.toArray());
    if (mode_ == ADD_ONLY_HOLD_SELECT) {
      setSelectedIndices(holdRows);
    }
    return (true);
  }
 
  /***************************************************************************
  **
  ** Delete a row from the list
  */
  
  public boolean deleteRows(int[] selectedRows) {
    List entries = client_.deleteRows(this, selectedRows);
    if (entries == null) {
      return (false);
    }
    list_.clearSelection();
    list_.setListData(entries.toArray());
    return (true);
  }
  
  /***************************************************************************
  **
  ** Edit a row from the list
  */
  
  public boolean editRow(int[] selectedRows) {
    List entries = client_.editRow(this, selectedRows);    
    if (entries == null) {
      return (false);
    }
    list_.clearSelection();
    list_.setListData(entries.toArray());
    return (true);
  }

  /***************************************************************************
  **
  ** Combine rows from the list
  */
  
  public boolean combineRows(int[] selectedRows) {
    List entries = client_.combineRows(this, selectedRows);    
    if (entries == null) {
      return (false);
    }
    list_.clearSelection();
    list_.setListData(entries.toArray());
    return (true);
  }  

  /***************************************************************************
  **
  ** Handle selections
  */
  
  public void valueChanged(ListSelectionEvent lse) {
    try {
      if (lse.getValueIsAdjusting()) {
        return;
      }      
      selectedRows_ = list_.getSelectedIndices();
      handleButtons(this.isEnabled());
    } catch (Exception ex) {
      hams_.getExceptionHandler().displayException(ex);
    }
    return;             
  }   
  
  /***************************************************************************
  **
  ** Handle enabling
  */
  
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    buttonA_.setEnabled(enabled);
    handleButtons(enabled);
    list_.setEnabled(enabled);
    return;             
  }     

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Handle the buttons
  */
  
  private void handleButtons(boolean amEnabled) {
    boolean haveRows = (selectedRows_.length > 0);
    boolean haveARow = (selectedRows_.length == 1);
    boolean haveManyRows = (selectedRows_.length >= 2);    
    if (buttonD_ != null) {
      buttonD_.setEnabled(haveRows && amEnabled);
      buttonD_.revalidate();
    }
    if (buttonE_ != null) {
      buttonE_.setEnabled(haveARow && amEnabled);
      buttonE_.revalidate();
    }
    if (buttonC_ != null) {
      buttonC_.setEnabled(haveManyRows && amEnabled);
      buttonC_.revalidate();
    }
    if (buttonExtra_ != null) {
      buttonExtra_.setEnabled(haveARow && amEnabled);
      buttonExtra_.revalidate();
    }  
    return;             
  }      
}
