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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.TabSource;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.undo.TemporalInputChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputChange;
import org.systemsbiology.biotapestry.timeCourse.TemporalInputRangeData;
import org.systemsbiology.biotapestry.timeCourse.TemporalRange;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ListWidget;
import org.systemsbiology.biotapestry.util.ListWidgetClient;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for choosing Time Course mapping
*/

public class TemporalInputTableManageDialog extends JDialog implements ListWidgetClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ListWidget lw_;
  private TemporalInputRangeData tird_;
  private DataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  private TabSource tSrc_;
  
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
  
  public TemporalInputTableManageDialog(UIComponentSource uics, DataAccessContext dacx, TabSource tSrc, UndoFactory uFac) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("titmd.title"), true);
    dacx_ = dacx;
    uics_ = uics;
    uFac_ = uFac;
    tSrc_ = tSrc;
    
    ResourceManager rMan = uics.getRMan();    
    setSize(500, 700);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    //
    // Create a list of the target genes available:
    //
    
    tird_ = dacx_.getTemporalRangeSrc().getTemporalInputRangeData();
    Iterator<TemporalRange> entries = tird_.getEntries();
    
    ArrayList<String> srcs = new ArrayList<String>();
    while (entries.hasNext()) {
      TemporalRange range = entries.next();
      srcs.add(range.getName());
    }       

    JLabel lab = new JLabel(rMan.getString("titmd.sources"));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    cp.add(lab, gbc);

    lw_ = new ListWidget(uics_.getHandlerAndManagerSource(), srcs, this);    
    UiUtil.gbcSet(gbc, 1, 0, 5, 5, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
    cp.add(lw_, gbc);
        
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TemporalInputTableManageDialog.this.setVisible(false);
          TemporalInputTableManageDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
      
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, 6, 6, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Add a row to the list
  */
  
  public List addRow(ListWidget widget) {
    ResourceManager rMan = dacx_.getRMan();
     
    String newEntry = 
      (String)JOptionPane.showInputDialog(uics_.getTopFrame(), 
                                          rMan.getString("titmd.newEntry"), 
                                          rMan.getString("titmd.newEntryTitle"),     
                                          JOptionPane.QUESTION_MESSAGE, null, 
                                          null, null);
    if (newEntry == null) {
      return (null);
    }
      
    if (newEntry.trim().equals("")) {
      Toolkit.getDefaultToolkit().beep();
      return (null);
    }
    
    if (tird_.getRange(newEntry) != null) {
      JOptionPane.showMessageDialog(uics_.getTopFrame(), 
                                    rMan.getString("titmd.nameNotUnique"), 
                                    rMan.getString("titmd.errorTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (null);
    } 
    
    UndoSupport support = uFac_.provideUndoSupport("undo.titmdadd", dacx_);
    
    TemporalRange newRange = new TemporalRange(newEntry, null, false);    
    TemporalInputChange tic = tird_.addEntry(newRange);
    support.addEdit(new TemporalInputChangeCmd(dacx_, tic));
 
    Iterator<TemporalRange> entries = tird_.getEntries();
    ArrayList<String> srcs = new ArrayList<String>();
    while (entries.hasNext()) {
      TemporalRange range = entries.next();
      srcs.add(range.getName());
    }
    
    support.addEvent(new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
    support.finish();
    return (srcs);
  }
  
  /***************************************************************************
  **
  ** Delete rows from the ORDERED list
  */
  
  public List deleteRows(ListWidget widget, int[] rows) {
    
    //
    // Undo/Redo support
    //
    
    UndoSupport support = uFac_.provideUndoSupport("undo.titmddelete", dacx_);     
    
    for (int i = 0; i < rows.length; i++) {
      String dropName = tird_.getEntry(rows[i]).getName();
      //FIX ME: Can't drop maps used in for rows to other tables!
      TemporalInputChange[] changes = tird_.dropMapsTo(dropName);
      for (int j = 0; j < changes.length; j++) {
        support.addEdit(new TemporalInputChangeCmd(dacx_, changes[j]));
      }      
      TemporalInputChange tic = tird_.dropEntry(rows[i]);
      support.addEdit(new TemporalInputChangeCmd(dacx_, tic));
      for (int j = i + 1; j < rows.length; j++) {
        if (rows[j] > rows[i]) {
          rows[j]--;
        }
      }
    }
        
    Iterator<TemporalRange> entries = tird_.getEntries();
    ArrayList<String> srcs = new ArrayList<String>();
    while (entries.hasNext()) {
      TemporalRange range = entries.next();
      srcs.add(range.getName());
    }
    
    support.addEvent(new GeneralChangeEvent(dacx_.getGenomeSource().getID(), GeneralChangeEvent.ChangeType.MODEL_DATA_CHANGE));
    support.finish();  
    return (srcs);    
  }

  /***************************************************************************
  **
  ** Edit a row from the list
  */
  
  public List editRow(ListWidget widget, int[] selectedRows) {
    if (selectedRows.length != 1) {
      throw new IllegalArgumentException();
    }
    
    TemporalRange targ = tird_.getEntry(selectedRows[0]);
    String name = targ.getName();
    ArrayList<String> list = new ArrayList<String>();
    list.add(name);
           
    TemporalInputDialog tid = TemporalInputDialog.temporalInputDialogWrapper(uics_, dacx_, tSrc_, list, true, uFac_);
    if (tid != null) {
      tid.setVisible(true);
    }

    Iterator<TemporalRange> entries = tird_.getEntries();
    ArrayList<String> srcs = new ArrayList<String>();
    while (entries.hasNext()) {
      TemporalRange range = entries.next();
      srcs.add(range.getName());
    }   

    return (srcs);
  }
  
  /***************************************************************************
  **
  ** Merge rows from the list
  */
  
  public List combineRows(ListWidget widget, int[] selectedRows) {
    // not used
    return (null);
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

}
