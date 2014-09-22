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

package org.systemsbiology.biotapestry.ui.dialogs.pertManage;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.perturb.PertFilterExpression;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitEditPanel;
import org.systemsbiology.biotapestry.ui.dialogs.utils.AnimatedSplitManagePanel;
import org.systemsbiology.biotapestry.util.AnimatedSplitPane;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.PendingEditTracker;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box summarizing available perturbation result sets
*/

public class PerturbationsManagementWindow extends JFrame implements PendingEditTracker, PertFilterExpressionJumpTarget {

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
  
  private BTState appState_;
  private DataAccessContext dacx_;
  private PerturbationData pd_;
  private JTabbedPane tabPane_;
  private HashSet<Integer> currPending_;
  private PMWindowListener pmwl_;
  private AnimatedSplitPane[] asp_;
  private HashMap<String, AnimatedSplitManagePanel> managePanels_;
  private HashMap<String, Integer> panelIndices_;
  private ArrayList<Integer> tabHistory_;
  private int currTabIndex_;
  private boolean ignoreButtonNav_;
  private JButton backBut_;
  private JButton forwardBut_;
  private PertFilterExpressionJumpTarget filterTarget_;
  private String filterTag_;
  private boolean editSubmissionActive_;

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
  
  public PerturbationsManagementWindow(BTState appState, DataAccessContext dacx, PerturbationData pd, PertFilterExpression pfe) {              
    super();
    appState_ = appState;
    dacx_ = dacx;
    URL ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/BioTapFab16White.gif");  
    setIconImage(new ImageIcon(ugif).getImage());
    setTitle(appState_.getRMan().getString("pertManage.title"));
    pd_ = pd;
    
    currPending_ = new HashSet<Integer>();
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    pmwl_ = new PMWindowListener();
    addWindowListener(pmwl_);
    editSubmissionActive_ = false;  
    int legacyModes = pd_.getExistingLegacyModes();
         
    ResourceManager rMan = appState_.getRMan();
    UiUtil.centerBigFrame(this, 1600, 1200, 0.8, 900);
    JPanel cp = (JPanel)getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    int cpRowNum = 0;
  
    PertDataPointManagePanel pdpm = new PertDataPointManagePanel(appState_, dacx_, this, pd_, this, legacyModes);
    filterTarget_ = pdpm;
    filterTag_ = pdpm.getTag();
    
    ArrayList<AnimatedSplitManagePanel> panelList = new ArrayList<AnimatedSplitManagePanel>();
    panelList.add(pdpm);
    panelList.add(new PertExperimentManagePanel(appState_, dacx_, this, pd_, this, this, legacyModes));
    panelList.add(new PertSrcDefsManagePanel(appState_, dacx_, this, pd_, this, this));
    panelList.add(new PertSrcsAndTargsManagePanel(appState_, dacx_, this, pd_, this, this));
    panelList.add(new PertInvestManagePanel(appState_, dacx_, this, pd_, this, this));
    panelList.add(new PertPropertiesManagePanel(appState_, dacx_, this, pd_, this, this));
    panelList.add(new PertMeasurementManagePanel(appState_, dacx_, this, pd_, this, this));
    panelList.add(new PertAnnotManagePanel(appState_, dacx_, this, pd_, this, this));
    panelList.add(new PertExpSetupManagePanel(appState_, dacx_, this, pd_, this, this));
    panelList.add(new PertMiscSetupManagePanel(appState_, dacx_, this, pd_, this, this));
    int numPan = panelList.size();
    tabPane_ = new JTabbedPane();
    managePanels_ = new HashMap<String, AnimatedSplitManagePanel>();
    panelIndices_ = new HashMap<String, Integer>();
    asp_ = new AnimatedSplitPane[numPan];
    for (int i = 0; i < numPan; i++) {
      AnimatedSplitManagePanel aspm = panelList.get(i);   
      tabPane_.addTab(rMan.getString("pertManage." + aspm.getTag()), aspm);
      asp_[i] = aspm.getSplit();
      managePanels_.put(aspm.getTag(), aspm);
      panelIndices_.put(aspm.getTag(), new Integer(i));
    }
       
    //
    // Navigation bar:
    //
    
    tabHistory_ = new ArrayList<Integer>();
    tabHistory_.add(new Integer(0));
    currTabIndex_ = 0;
    ignoreButtonNav_ = false;
    
    ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/Back24.gif"); 
    ImageIcon back = new ImageIcon(ugif);
    backBut_ = new FixedJButton(back, 10);
    backBut_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (currTabIndex_ <= 0) {
            return;
          }
          currTabIndex_--;
          ignoreButtonNav_ = true;
          tabPane_.setSelectedIndex(tabHistory_.get(currTabIndex_).intValue());
          ignoreButtonNav_ = false;
          backBut_.setEnabled(currTabIndex_ > 0);
          forwardBut_.setEnabled(currTabIndex_ < (tabHistory_.size() - 1));
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    backBut_.setEnabled(false);
       
    ugif = getClass().getResource("/org/systemsbiology/biotapestry/images/Forward24.gif"); 
    ImageIcon forward = new ImageIcon(ugif);
    forwardBut_ = new FixedJButton(forward, 10);
    forwardBut_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (currTabIndex_ >= (tabHistory_.size() - 1)) {
            return;
          }
          currTabIndex_++;
          ignoreButtonNav_ = true;
          tabPane_.setSelectedIndex(tabHistory_.get(currTabIndex_).intValue());
          ignoreButtonNav_ = false;
          backBut_.setEnabled(currTabIndex_ > 0);
          forwardBut_.setEnabled(currTabIndex_ < (tabHistory_.size() - 1));
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    forwardBut_.setEnabled(false);         
      
    Box topBarPanel = Box.createHorizontalBox();
    topBarPanel.add(Box.createHorizontalStrut(0)); 
    topBarPanel.add(backBut_);
    topBarPanel.add(Box.createHorizontalStrut(2)); 
    topBarPanel.add(forwardBut_);
    topBarPanel.add(Box.createHorizontalGlue());  
         
    UiUtil.gbcSet(gbc, 0, cpRowNum++, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(topBarPanel, gbc);   
        
    //
    // To handle J1.4 split pane bug nonsense as well as track tab history:
    //
    
    tabPane_.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent ev) {
        try {
          int sel = tabPane_.getSelectedIndex();
          AnimatedSplitPane asp = asp_[sel];
          if (asp != null) {
            asp.doFixie();
          }
          asp_[sel] = null;
          if (ignoreButtonNav_) {
            return;
          }
          // Kill history above current history index, then add to end:
          int startIndex = tabHistory_.size() - 1;
          if (currTabIndex_ < startIndex) {
            for (int i = startIndex; i > currTabIndex_; i--) {
              tabHistory_.remove(i);
            }
          }
          tabHistory_.add(new Integer(sel));
          currTabIndex_++;
          backBut_.setEnabled(currTabIndex_ > 0);
          forwardBut_.setEnabled(currTabIndex_ < (tabHistory_.size() - 1)); 
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
 
    //
    // Tabs:
    //
    
    UiUtil.gbcSet(gbc, 0, cpRowNum, 1, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cpRowNum += 10;
    cp.add(tabPane_, gbc);
    
    //
    // Build the button panel:
    //

    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.close"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        pmwl_.windowClosing(null);
      }
    });     

    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
 
    //
    // Build the dialog:
    //
    
    buttonPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
    UiUtil.gbcSet(gbc, 0, cpRowNum, 1, 1, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(buttonPanel, gbc);    
    setLocationRelativeTo(appState_.getTopFrame());
    
    if (pfe != null) {
      filterTarget_.jumpWithNewFilter(pfe);
    }   
  }
 
  /***************************************************************************
  **
  ** Let us know before edit is submitted
  ** 
  */
 
  public void editSubmissionBegins() {
    editSubmissionActive_ = true;  
    return;
  }
  
  /***************************************************************************
  **
  ** Let us know after edit submission is done
  ** 
  */ 
  
  public void editSubmissionEnds() {
    editSubmissionActive_ = false;
    return;
  }

  /***************************************************************************
  **
  ** Tell us to refresh our view
  ** 
  */
  
  public void modelChanged() {
    // So perturbation data presentation keeps track of undo/redo
    if (editSubmissionActive_) {
      return;
    }
    ((PertDataPointManagePanel)filterTarget_).doScalingUpdate();
    Iterator<AnimatedSplitManagePanel> mpit = managePanels_.values().iterator();
    while (mpit.hasNext()) {
      AnimatedSplitManagePanel asmp = mpit.next();
      asmp.haveAChange(false);
    }
    editSubmissionActive_ = false;
    return;
  }
  
  /***************************************************************************
  **
  ** Jump with a new filter
  ** 
  */
  
  public void jumpWithNewFilter(PertFilterExpression pfe) {
    if (((PertDataPointManagePanel)filterTarget_).havePendingEdit()) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(this, rMan.getString("pertManage.editingCannotJump"),
                                    rMan.getString("pertManage.editingCannotJumpTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    filterTarget_.jumpWithNewFilter(pfe);
    jumpToRemoteEdit(filterTag_, null, null);
    return;
  }
 
  /***************************************************************************
  **
  ** Answers if we have a pending edit
  ** 
  */
  
  public boolean havePendingEdit() {
    return (!currPending_.isEmpty());
  }
  
  /***************************************************************************
  **
  ** Let us know if we have a pending edit.  We use this to asterisk the tab 
  ** and record the pending state
  ** 
  */
  
  public void editIsPending(String key) {
    int currIndex = tabPane_.getSelectedIndex();
    Integer ciObj = new Integer(currIndex);
    if (currPending_.contains(ciObj)) {
      return;
    }
    String title = tabPane_.getTitleAt(currIndex);
    title = title.concat("*");
    tabPane_.setTitleAt(currIndex, title);
    currPending_.add(ciObj);
    return;
  }
  
  /***************************************************************************
  **
  ** Let us know if we have a completed edit.  We use this to asterisk the tab 
  ** and record the pending state
  ** 
  */
  
  public void editIsComplete(String key, int what) {
    boolean mustDie = (what == AnimatedSplitEditPanel.MERGE_MODE);
    // Has to update scaling first, since standard have a change
    // assumes the scaling is hunky-dory!
    ((PertDataPointManagePanel)filterTarget_).doScalingUpdate();
    Iterator<AnimatedSplitManagePanel> mpit = managePanels_.values().iterator();
    while (mpit.hasNext()) {
      AnimatedSplitManagePanel asmp = mpit.next();
      asmp.haveAChange(mustDie);
    }  
    Integer completedIndex = panelIndices_.get(key);
    if (!currPending_.contains(completedIndex)) {
      return;
    }
    int currIndex = completedIndex.intValue();
    String title = tabPane_.getTitleAt(currIndex);
    title = title.substring(0, title.length() - 1);
    tabPane_.setTitleAt(currIndex, title);
    currPending_.remove(completedIndex);
    return;
  }
  
  /***************************************************************************
  **
  ** Let us know if we have deletion.
  ** 
  */
  
  public void itemDeleted(String key) {
    if (key.equals(PertMiscSetupManagePanel.MEAS_SCALE_KEY)) {
      ((PertDataPointManagePanel)filterTarget_).doScalingUpdate();
    } 
    Iterator<AnimatedSplitManagePanel> mpit = managePanels_.values().iterator();
    while (mpit.hasNext()) {
      AnimatedSplitManagePanel asmp = mpit.next();
      asmp.haveAChange(false);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Drop all pending edits
  ** 
  */
  
  public void dropAllPendingEdits() {  
    Iterator<AnimatedSplitManagePanel> mpit = managePanels_.values().iterator();
    while (mpit.hasNext()) {
      AnimatedSplitManagePanel asmp = mpit.next();
      asmp.dropPendingEdits();
    }
    return;
  }
 
  /***************************************************************************
  **
  ** Let us know if we have a cancelled edit.
  */ 
 
  
  public void editIsCancelled(String key) {
    Integer completedIndex = panelIndices_.get(key);
    if (!currPending_.contains(completedIndex)) {
      return;
    }
    int currIndex = completedIndex.intValue();
    String title = tabPane_.getTitleAt(currIndex);
    title = title.substring(0, title.length() - 1);
    tabPane_.setTitleAt(currIndex, title);
    currPending_.remove(completedIndex);
    return;
  }
  
  /***************************************************************************
  **
  ** We do not deal with pushed and popped edits
  ** 
  */
  
  public void editIsPushed(String key) {
    return;
  }
  
  /***************************************************************************
  **
  ** Let us know if we need to jump to another editor
  ** 
  */
  
  public void jumpToRemoteEdit(String key, String tableTarg, String rowTarg) {
    AnimatedSplitManagePanel asmp = managePanels_.get(key);
    if (asmp.havePendingEdit()) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(this, rMan.getString("pertManage.editingCannotJump"),
                                    rMan.getString("pertManage.editingCannotJumpTitle"), 
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    asmp.setTableSelection(tableTarg, rowTarg);
    tabPane_.setSelectedComponent(asmp);
    return;
  }
  
  /***************************************************************************
  **
  ** Handle Window open and close
  */
  
  private class PMWindowListener extends WindowAdapter {
    
    public void windowClosing(WindowEvent e) {
      try {
        if (!currPending_.isEmpty()) {
          ResourceManager rMan = appState_.getRMan();
          int ok = JOptionPane.showConfirmDialog(PerturbationsManagementWindow.this, 
                                                 rMan.getString("pertManage.pendingEdits"), 
                                                 rMan.getString("pertManage.pendingEditsTitle"),
                                                 JOptionPane.YES_NO_OPTION);
          if (ok != JOptionPane.YES_OPTION) {
            return;
          }
        } 
        
        appState_.getCommonView().clearPerturbationsManagementWindow();       
        PerturbationsManagementWindow.this.setVisible(false);
        PerturbationsManagementWindow.this.dispose();  
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return;
    }
    
    public void windowOpened(WindowEvent e) {
      try {
        AnimatedSplitPane asp = asp_[0];
        if (asp != null) {
          asp.doFixie();
        }
        asp_[0] = null;
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return;
    }
  }
}
