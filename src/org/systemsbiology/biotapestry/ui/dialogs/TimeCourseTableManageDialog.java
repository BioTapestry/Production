/*
**    Copyright (C) 2003-2014 Institute for Systems Biology 
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.undo.TimeCourseChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.timeCourse.GeneTemplateEntry;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseChange;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseData;
import org.systemsbiology.biotapestry.timeCourse.TimeCourseGene;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ListWidget;
import org.systemsbiology.biotapestry.util.ListWidgetClient;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;

/****************************************************************************
**
** Dialog box for choosing Time Course mapping
*/

public class TimeCourseTableManageDialog extends JDialog implements ListWidgetClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ListWidget lw_;
  private TimeCourseData tcd_;
  private BTState appState_;
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
  
  public TimeCourseTableManageDialog(BTState appState, DataAccessContext dacx) {     
    super(appState.getTopFrame(), appState.getRMan().getString("tctmd.title"), true);
    appState_ = appState;
    dacx_ = dacx;
    
    ResourceManager rMan = appState.getRMan();    
    setSize(700, 700);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    //
    // Create a list of the target genes available:
    //
    
    tcd_ = appState_.getDB().getTimeCourseData();
    Iterator<TimeCourseGene> genes = tcd_.getGenes();
    ArrayList<String> srcs = new ArrayList<String>();
    while (genes.hasNext()) {
      TimeCourseGene gene = genes.next();
      srcs.add(gene.getName());
    } 
    Collections.sort(srcs, String.CASE_INSENSITIVE_ORDER);   
     
    JLabel lab = new JLabel(rMan.getString("tctmd.sources"));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    cp.add(lab, gbc);

    JButton buttonPE = new JButton(rMan.getString("tctmd.pertEdit"));
     buttonPE.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TimeCourseGene targ = tcd_.getTimeCourseDataCaseInsensitive((String)(lw_.getSelectedObjects()[0]));
          PerturbExpressionEntryDialog tced = PerturbExpressionEntryDialog.launchIfPerturbSourcesExist(appState_, dacx_, targ.getName());
          if (tced != null) {
            tced.setVisible(true);
          }
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });    
    
    lw_ = new ListWidget(appState_, srcs, this, ListWidget.FULL_MODE, buttonPE);    
    UiUtil.gbcSet(gbc, 1, 0, 5, 5, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
    cp.add(lw_, gbc);
        
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          TimeCourseTableManageDialog.this.setVisible(false);
          TimeCourseTableManageDialog.this.dispose();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });     
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    //<String>
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, 6, 6, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState_.getTopFrame());
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
  
  public List<String> addRow(ListWidget widget) {
    ResourceManager rMan = appState_.getRMan();
    
    //
    // If data table is not set up, do it right now:
    //
    
    if (!tcd_.hasGeneTemplate()) {        
      TimeCourseSetupDialog tcsd = TimeCourseSetupDialog.timeSourceSetupDialogWrapper(appState_, dacx_);
      if (tcsd == null) {
        return (null);
      }
      tcsd.setVisible(true);
      if (!tcd_.hasGeneTemplate()) {
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      rMan.getString("tcmdNoTemplate.message"), 
                                      rMan.getString("tcmdNoTemplate.title"),
                                      JOptionPane.ERROR_MESSAGE);
         return (null);
      } 
    }
 
    String newGene = 
      (String)JOptionPane.showInputDialog(appState_.getTopFrame(), 
                                          rMan.getString("tctmd.newGene"), 
                                          rMan.getString("tctmd.newGeneTitle"),     
                                          JOptionPane.QUESTION_MESSAGE, null, 
                                          null, null);
    if (newGene == null) {
      return (null);
    }
    
    newGene = newGene.trim();
    
    if (newGene.equals("")) {
      Toolkit.getDefaultToolkit().beep();
      return (null);
    }
   
    if (!tcd_.nameIsUnique(newGene)) {
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                    rMan.getString("tctmd.nameNotUnique"), 
                                    rMan.getString("tctmd.errorTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (null);
    }      
     
    UndoSupport support = new UndoSupport(appState_, "undo.tctmdadd"); 
    
    Iterator<GeneTemplateEntry> template = tcd_.getGeneTemplate();
    TimeCourseGene tcg = new TimeCourseGene(appState_, newGene, template);
    TimeCourseChange tcc = tcd_.addTimeCourseGene(tcg);
    support.addEdit(new TimeCourseChangeCmd(appState_, dacx_, tcc));    
 
    Iterator<TimeCourseGene> genes = tcd_.getGenes();
    ArrayList<String> srcs = new ArrayList<String>();
    while (genes.hasNext()) {
      TimeCourseGene gene = genes.next();
      srcs.add(gene.getName());
    }
    Collections.sort(srcs, String.CASE_INSENSITIVE_ORDER);   
    
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
    support.finish();
    return (srcs);
  }
  
  /***************************************************************************
  **
  ** Delete rows from the ORDERED list
  */
  
  public List<String> deleteRows(ListWidget widget, int[] rows) {
    
    //
    // Undo/Redo support
    //
    
    UndoSupport support = new UndoSupport(appState_, "undo.tctmddelete");    
    
    for (int i = 0; i < rows.length; i++) {
      String geneName = (String)widget.getElementAt(rows[i]);
      TimeCourseChange[] changes = tcd_.dropMapsTo(geneName);
      for (int j = 0; j < changes.length; j++) {
        support.addEdit(new TimeCourseChangeCmd(appState_, dacx_, changes[j]));
      }
      TimeCourseChange tcc = tcd_.dropGene(geneName);
      support.addEdit(new TimeCourseChangeCmd(appState_, dacx_, tcc));
      for (int j = i + 1; j < rows.length; j++) {
        if (rows[j] > rows[i]) {
          rows[j]--;
        }
      }
    }
        
    Iterator<TimeCourseGene> genes = tcd_.getGenes();
    ArrayList<String> srcs = new ArrayList<String>();
    while (genes.hasNext()) {
      TimeCourseGene gene = genes.next();
      srcs.add(gene.getName());
    }
    Collections.sort(srcs, String.CASE_INSENSITIVE_ORDER);   
    
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.MODEL_DATA_CHANGE));
    support.finish();    
    return (srcs);    
  }

  /***************************************************************************
  **
  ** Edit a row from the list
  */
  
  public List<String> editRow(ListWidget widget, int[] selectedRows) {
    ResourceManager rMan = appState_.getRMan();
    if (selectedRows.length != 1) {
      throw new IllegalArgumentException();
    }
           
    //
    // If data table is not set up, do it right now:
    //
    
    if (!tcd_.hasGeneTemplate()) {
      TimeCourseSetupDialog tcsd = TimeCourseSetupDialog.timeSourceSetupDialogWrapper(appState_, dacx_);
      if (tcsd == null) {
        return (null);
      }
      tcsd.setVisible(true);
      if (!tcd_.hasGeneTemplate()) {
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      rMan.getString("tcmdNoTemplate.message"), 
                                      rMan.getString("tcmdNoTemplate.title"),
                                      JOptionPane.ERROR_MESSAGE);
         return (null);
      } 
    }
    
    String geneName = (String)widget.getElementAt(selectedRows[0]);
    ArrayList<String> list = new ArrayList<String>();
    list.add(geneName);
           
    TimeCourseEntryDialog tced = new TimeCourseEntryDialog(appState_, dacx_, list, true);
    tced.setVisible(true);

    Iterator<TimeCourseGene> genes = tcd_.getGenes();
    ArrayList<String> srcs = new ArrayList<String>();
    while (genes.hasNext()) {
      TimeCourseGene gene = genes.next();
      srcs.add(gene.getName());
    } 
    Collections.sort(srcs, String.CASE_INSENSITIVE_ORDER);      

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
