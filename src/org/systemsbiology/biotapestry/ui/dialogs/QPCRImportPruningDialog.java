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


package org.systemsbiology.biotapestry.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.ListWidget;
import org.systemsbiology.biotapestry.util.ListWidgetClient;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for selecting QPCR imports
*/

public class QPCRImportPruningDialog extends JDialog implements ListWidgetClient {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private ListWidget lw_;
  private Set candidates_;
  private ArrayList candidateList_;
  private ArrayList candidateDisplayList_;  
  private HashMap candidateMap_;
  private boolean haveResult_;
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
  
  public QPCRImportPruningDialog(BTState appState, Set candidates) {     
    super(appState.getTopFrame(), appState.getRMan().getString("qipd.title"), true);
    appState_ = appState;
    candidates_ = candidates;
    
    ResourceManager rMan = appState.getRMan(); 
    setSize(500, 700);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    //
    // The list of the import genes available:
    //
    
    candidateList_ = new ArrayList(candidates);
    Collections.sort(candidateList_, new CandidateSorter());
    candidateMap_ = new HashMap();
    candidateDisplayList_ = new ArrayList(candidateList_);    
    Iterator cit = candidateList_.iterator();
    while (cit.hasNext()) {
      PerturbationData.QpcrSourceSet source = (PerturbationData.QpcrSourceSet)cit.next();
      ArrayList equal = new ArrayList();
      equal.add(source);
      candidateMap_.put(source.name, equal); 
    } 
    
    JLabel lab = new JLabel(rMan.getString("qtmd.sources"));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);       
    cp.add(lab, gbc);

    lw_ = new ListWidget(appState, candidateDisplayList_, this, ListWidget.DELETE_COMBINE);    
    UiUtil.gbcSet(gbc, 1, 0, 5, 5, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);       
    cp.add(lw_, gbc);
        
    //
    // Build the button panel:
    //
  
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (stashResults(true)) {
            QPCRImportPruningDialog.this.setVisible(false);
            QPCRImportPruningDialog.this.dispose();
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
            QPCRImportPruningDialog.this.setVisible(false);
            QPCRImportPruningDialog.this.dispose();
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
    UiUtil.gbcSet(gbc, 0, 6, 7, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(appState.getTopFrame());
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
    // not used
    return (null);
  }

  /***************************************************************************
  **
  ** Delete rows from the ORDERED list
  */
  
  public List deleteRows(ListWidget widget, int[] rows) {
    for (int i = 0; i < rows.length; i++) {
      candidateDisplayList_.remove(rows[i]);
      PerturbationData.QpcrSourceSet ditchName = (PerturbationData.QpcrSourceSet)candidateList_.get(rows[i]);
      candidateList_.remove(rows[i]);      
      candidateMap_.remove(ditchName.name);
      for (int j = i + 1; j < rows.length; j++) {
        if (rows[j] > rows[i]) {
          rows[j]--;
        }
      }
    }
    return (candidateDisplayList_);
  }

  /***************************************************************************
  **
  ** Edit a row from the list
  */
  
  public List editRow(ListWidget widget, int[] selectedRows) {
    // not used
    return (null);
  }
  
  /***************************************************************************
  **
  ** Merge rows from the list
  */
  
  public List combineRows(ListWidget widget, int[] rows) {
    if (rows.length < 2) {
      throw new IllegalArgumentException();
    }
    int masterIndex = rows[0];
    String masterName = ((PerturbationData.QpcrSourceSet)candidateList_.get(masterIndex)).name;
    ArrayList same = (ArrayList)candidateMap_.get(masterName);
    StringBuffer buf = new StringBuffer();
    buf.append(masterName);
    buf.append(" [");
    for (int i = 0; i < same.size(); i++) {
      buf.append(((PerturbationData.QpcrSourceSet)same.get(i)).name);
      buf.append(", ");
    }
    for (int i = 1; i < rows.length; i++) {
      PerturbationData.QpcrSourceSet ditchName = (PerturbationData.QpcrSourceSet)candidateList_.get(rows[i]);
      same.add(ditchName);
      buf.append(ditchName.name);
      if (i < rows.length - 1) {
        buf.append(", ");
      }
      candidateList_.remove(rows[i]);
      candidateDisplayList_.remove(rows[i]);
      candidateMap_.remove(ditchName.name);
      for (int j = i + 1; j < rows.length; j++) {
        if (rows[j] > rows[i]) {
          rows[j]--;
        }
      }
    }
    buf.append("]");
    candidateDisplayList_.set(masterIndex, buf.toString());
    return (candidateDisplayList_);
  }
  
  /***************************************************************************
  **
  ** Get the set result.
  ** 
  */
  
  public Map getResult() {
    return (candidateMap_);
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
  
  private class CandidateSorter implements Comparator<PerturbationData.QpcrSourceSet> {
    
    public int compare(PerturbationData.QpcrSourceSet s1, PerturbationData.QpcrSourceSet s2) {
      return (s1.name.compareToIgnoreCase(s2.name));
    }
    
  }  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation
  ** 
  */
  
  private boolean stashResults(boolean ok) {
    if (ok) {
      haveResult_ = true;
    } else {
      haveResult_ = false;
    }
    return (true);
  }
}
