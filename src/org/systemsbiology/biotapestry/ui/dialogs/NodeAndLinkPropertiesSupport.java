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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.cmd.PadCalculatorToo;
import org.systemsbiology.biotapestry.cmd.undo.GenomeChangeCmd;
import org.systemsbiology.biotapestry.db.DataAccessContext;
import org.systemsbiology.biotapestry.genome.DBNode;
import org.systemsbiology.biotapestry.genome.FullGenomeHierarchyOracle;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.genome.GenomeChange;
import org.systemsbiology.biotapestry.genome.GenomeItemInstance;
import org.systemsbiology.biotapestry.genome.Linkage;
import org.systemsbiology.biotapestry.genome.LinkageInstance;
import org.systemsbiology.biotapestry.genome.Node;
import org.systemsbiology.biotapestry.genome.NodeInstance;
import org.systemsbiology.biotapestry.genome.TopOfTheHeap;
import org.systemsbiology.biotapestry.ui.FontManager;
import org.systemsbiology.biotapestry.ui.NodeProperties;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.LineBreaker;
import org.systemsbiology.biotapestry.util.MultiLineRenderSupport;
import org.systemsbiology.biotapestry.util.ObjChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.TriStateJComboBox;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoSupport;


/****************************************************************************
**
** Need to refactor the NodePropertiesDialog and GenePropertiesDialog.  Maybe
** use a common base class?  Short term: put common support code here and
** use composition.
*/

public class NodeAndLinkPropertiesSupport implements DocumentListener {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private static final int ROOT_TARGET_ = 0;
  private static final int LOCAL_TARGET_ = 1;
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private BTState appState_;
  private DataAccessContext dacx_;
  private String nodeID_;
  private String genomeKey_;
  
  private JPanel myURLPanel_;
  private boolean calcURL_;
  private int selectedURLIndex_;
  private JList urlList_;
  private FixedJButton buttonURLAdd_;
  private FixedJButton buttonURLCxEdit_;  
  private FixedJButton buttonURLDel_;
  private FixedJButton buttonURLEdit_;
  private FixedJButton buttonURLDown_;     
  private FixedJButton buttonURLUp_;
  private JTextField urlField_;
  private JLabel addEditLabel_;
  
  private JComboBox freeTextTarget_;
  private JComboBox whichFreeTextLink_;
  private JTextArea freeTextField_;
  private int lastFreeTextTarg_;
  private String lastFreeTextLinkTarg_;
  private HashMap allFreeText_;
  private JScrollPane freeScroll_;
  private boolean listMod_;
  private Set forLinks_;
  
  
  private JComboBox urlListTarget_;
  private JComboBox whichUrlLink_;
  private int lastUrlTarg_;
  private String lastUrlLinkTarg_;
  private ArrayList urlListData_;
  private HashMap allUrlLists_;
  private Set urlsForLinks_;
  private JScrollPane urlScroll_;
  private boolean urlListMod_;
  private boolean editing_;
  
  private JCheckBox doOverFontBox_;
  private TriStateJComboBox doOverFontMultiCombo_;
  private FontSettingPanel fsp_;  
    
  private JCheckBox doMultiLine_;   
  private JTextArea lbp_;
  private NewLineOnlyDocument breakDoc_;

  private BreakState globalCumulativeBreak_;  
  private BreakState globalBreak_;
  private BreakState localBreak_;
  private boolean doLocalBreak_;
  
  private JComboBox orientCombo_;
  private JComboBox activityCombo_;
  private JLabel levelLabel_;    
  private JTextField activityLevel_; 
 
  private JCheckBox bigBox_;
  private TriStateJComboBox bigBoxMultiCombo_;
  private JLabel extraPadLabel_;  
  private JComboBox extraPadCombo_; 
  
  private JLabel extraGrowthLabel_;
  private JComboBox extraGrowthCombo_;

  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VISIBLE CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  NodeAndLinkPropertiesSupport(BTState appState, DataAccessContext dacx, String nodeID) { 
    appState_ = appState;
    dacx_ = dacx;
    nodeID_ = nodeID;
    genomeKey_ = dacx_.getGenomeID();
  }
  
  /***************************************************************************
  **
  ** Constructor. This class is now getting used for non-node situations!
  */ 
  
  NodeAndLinkPropertiesSupport(BTState appState, DataAccessContext dacx) { 
    appState_ = appState;
    dacx_ = dacx;
    genomeKey_ = dacx_.getGenomeID();
  }
  
  /***************************************************************************
  **
  ** Constructor. This class is now getting used for non-node situations!

  
  NodeAndLinkPropertiesSupport(BTState appState, DataAccessContext dacx) { 
    appState_ = appState;
    dacx_ = dacx;
  }  
     
  ////////////////////////////////////////////////////////////////////////////
  //
  // PACKAGE-VISIBLE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Check if desired activity bounds can be allowed.  MERGE THIS WITH THE METHOD BELOW!!!!
  */
  
  boolean checkActivityBounds(GenomeItemInstance.ActivityTracking actTrack, GenomeItemInstance.ActivityState nias) {
  
    ResourceManager rMan = appState_.getRMan();
    
    int newActivity = nias.activityState;
            
    //
    // Make sure we are OK wrt our parent (if we have one):
    //
   
    if (actTrack.parentActivity != null) {
      int parentNodeActivityVal = actTrack.parentActivity.intValue();
      if (!NodeInstance.activityLevelAllowedInChild(parentNodeActivityVal, newActivity)) {
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      rMan.getString("nodeProp.badActivityValViaParent"), 
                                      rMan.getString("nodeProp.badActivityTitle"),
                                      JOptionPane.ERROR_MESSAGE);         
        return (false);
      }
      if ((actTrack.parentActivityLevel != Double.NEGATIVE_INFINITY) && (newActivity == GenomeItemInstance.VARIABLE)) {
        if (nias.activityLevel.doubleValue() > actTrack.parentActivityLevel) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                        rMan.getString("nodeProp.badActivityLevelViaParent"), 
                                        rMan.getString("nodeProp.badActivityTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
          return (false);               
        }
      }
    }
    
    //
    // Make sure we are OK wrt our children:
    //
    
    if (actTrack.childActivities != null) {
      Iterator cait = actTrack.childActivities.iterator();
      while (cait.hasNext()) {
        Integer childActivity = (Integer)cait.next();
        int childActivityVal = childActivity.intValue();        
        if (!NodeInstance.activityLevelAllowedInChild(newActivity, childActivityVal)) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                        rMan.getString("nodeProp.badActivityValViaChild"), 
                                        rMan.getString("nodeProp.badActivityTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
          return (false);
        }       
      }
      if ((actTrack.maxChildLevel != Double.NEGATIVE_INFINITY) && (newActivity == GenomeItemInstance.VARIABLE)) {
        if (nias.activityLevel.doubleValue() < actTrack.maxChildLevel) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                        rMan.getString("nodeProp.badActivityLevelViaChild"), 
                                        rMan.getString("nodeProp.badActivityTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
          return (false);               
        }
      }     
    }
    
    return (true);
  }
  
  /***************************************************************************
  **
  ** Check if desired activity bounds can be allowed FOR LINKS
  */
  
  boolean checkActivityBounds(GenomeItemInstance.ActivityTracking trackInfo, int newActivity, double newLevel) {
  
    ResourceManager rMan = appState_.getRMan();
    
    //
    // Make sure we are OK wrt our parent (if we have one):
    //
   
    if (trackInfo.parentActivity != null) {
      int parentActivityVal = trackInfo.parentActivity.intValue();
      if (!LinkageInstance.activityLevelAllowedInChild(parentActivityVal, newActivity)) {
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      rMan.getString("lprop.badActivityValViaParent"), 
                                      rMan.getString("lprop.badActivityTitle"),
                                      JOptionPane.ERROR_MESSAGE);         
        return (false);
      }
      if ((trackInfo.parentActivityLevel != Double.NEGATIVE_INFINITY) && (newActivity == LinkageInstance.VARIABLE)) {
        if (newLevel > trackInfo.parentActivityLevel) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                        rMan.getString("lprop.badActivityLevelViaParent"), 
                                        rMan.getString("lprop.badActivityTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
          return (false);               
        }
      }
    }
    
    //
    // Make sure we are OK wrt our children:
    //
    
    if (trackInfo.childActivities != null) {
      Iterator cait = trackInfo.childActivities.iterator();
      while (cait.hasNext()) {
        Integer childActivity = (Integer)cait.next();
        int childActivityVal = childActivity.intValue();
        if (!LinkageInstance.activityLevelAllowedInChild(newActivity, childActivityVal)) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                        rMan.getString("lprop.badActivityValViaChild"), 
                                        rMan.getString("lprop.badActivityTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
          return (false);
        }       
      }
      if ((trackInfo.maxChildLevel != Double.NEGATIVE_INFINITY) && (newActivity == LinkageInstance.VARIABLE)) {
        if (newLevel < trackInfo.maxChildLevel) {
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                        rMan.getString("lprop.badActivityLevelViaChild"), 
                                        rMan.getString("lprop.badActivityTitle"),
                                        JOptionPane.ERROR_MESSAGE);         
          return (false);               
        }
      }     
    }
    
    return (true);
  } 
 
  /***************************************************************************
  **
  ** Build a tab for free text
  ** 
  */
  
  public JPanel buildTextTab(Set forLinks, boolean forRoot) {

    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = appState_.getRMan();
    
    //
    // Build the model key text field
    //

    int rowNum = 0;
    JLabel label = new JLabel(rMan.getString("nprop.freeTextPrompt"), JLabel.LEFT);
    UiUtil.gbcSet(gbc, 0, rowNum++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.E, 1.0, 0.0);
    retval.add(label, gbc);    
   
    
    //
    // Always build it, only use it if needed:
    //
    
    freeTextTarget_ = new JComboBox(buildTargetChoices());
    lastFreeTextTarg_ = ROOT_TARGET_;
    allFreeText_ = new HashMap();
    
    if (!forRoot) {
      label = new JLabel(rMan.getString("nprop.freeTextTarget"), JLabel.LEFT);
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.E, 1.0, 0.0);
      retval.add(label, gbc);  
    
      freeTextTarget_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            manageDescription();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.E, 1.0, 0.0);
      retval.add(freeTextTarget_, gbc);
    }
    
    if (forLinks != null) {
      forLinks_ = forLinks;
      label = new JLabel(rMan.getString("nprop.whichLink"), JLabel.LEFT);
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.E, 1.0, 0.0);
      retval.add(label, gbc);  
    
      whichFreeTextLink_ = new JComboBox(buildLinkChoices(forLinks_, true));
      whichFreeTextLink_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            manageDescriptionForLinks();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      lastFreeTextLinkTarg_ = ((ObjChoiceContent)whichFreeTextLink_.getSelectedItem()).val;
      listMod_ = false;
 
      UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.E, 1.0, 0.0);
      retval.add(whichFreeTextLink_, gbc);    
    }

    freeTextField_ = new JTextArea();
    freeTextField_.setEditable(true);
    freeScroll_ = new JScrollPane(freeTextField_); 

    UiUtil.gbcSet(gbc, 0, rowNum, 2, 10, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(freeScroll_, gbc);          
    
    return (retval);
  } 
 
  /***************************************************************************
  **
  ** Manage a description shift
  ** 
  */

  private void manageDescription() {
       
    ChoiceContent targ = (ChoiceContent)freeTextTarget_.getSelectedItem();
    if (lastFreeTextTarg_ == targ.val) {
      return;
    }
    
    //
    // Link case:
    //
    
    if (forLinks_ != null) {
      listMod_ = true;
      UiUtil.replaceComboItems(whichFreeTextLink_, buildLinkChoices(forLinks_, (targ.val == ROOT_TARGET_)));
      listMod_ = false;
      lastFreeTextTarg_ = targ.val;
      whichFreeTextLink_.setSelectedIndex(0);    
      return;
    }
    
    //
    // node case:
    //
    
    String currText = freeTextField_.getText();
    String useID = (lastFreeTextTarg_ == ROOT_TARGET_) ? GenomeItemInstance.getBaseID(nodeID_) : nodeID_;
    allFreeText_.put(useID, currText);
    
    useID = (targ.val == ROOT_TARGET_) ? GenomeItemInstance.getBaseID(nodeID_) : nodeID_;
  
    currText = (String)allFreeText_.get(useID);
    freeTextField_.setText((currText == null) ? "" : currText);  
    freeTextField_.setCaretPosition(0);
    freeScroll_.getVerticalScrollBar().setValue(0);
    lastFreeTextTarg_ = targ.val;
    return;
  }
   
  /***************************************************************************
  **
  ** Manage a description shift for links
  ** 
  */

  private void manageDescriptionForLinks() {
    if (listMod_) {
      return;
    }
   
    ObjChoiceContent targ = (ObjChoiceContent)whichFreeTextLink_.getSelectedItem();
 
    String currText = freeTextField_.getText();
    allFreeText_.put(lastFreeTextLinkTarg_, currText);      
    currText = (String)allFreeText_.get(targ.val);
    freeTextField_.setText((currText == null) ? "" : currText);  
    freeTextField_.setCaretPosition(0);
    freeScroll_.getVerticalScrollBar().setValue(0);
    lastFreeTextLinkTarg_ = targ.val;
    return;
  }
 
  /***************************************************************************
  **
  ** Build the URL and free text target choices
  ** 
  */

  private Vector<ChoiceContent> buildTargetChoices() {
    Vector<ChoiceContent> retval = new Vector<ChoiceContent>();
    retval.add(new ChoiceContent(dacx_.rMan.getString("nprop.rootDefaultTarget"), ROOT_TARGET_));
    retval.add(new ChoiceContent(dacx_.rMan.getString("nprop.localTarget"), LOCAL_TARGET_));
    return (retval);
  }

  /***************************************************************************
  **
  ** Build the URL and free text link choices
  ** 
  */

  private Vector<ObjChoiceContent> buildLinkChoices(Set<String> linkIDs, boolean forRoot) {
    Vector<ObjChoiceContent> retval = new Vector<ObjChoiceContent>();
    HashSet<String> seen = new HashSet<String>();
    Genome genome = (forRoot) ? dacx_.getDBGenome() : dacx_.getGenomeSource().getGenome(genomeKey_);
    Iterator<String> lit = linkIDs.iterator();
    while (lit.hasNext()) {
      String linkID = lit.next();
      linkID = (forRoot) ? GenomeItemInstance.getBaseID(linkID) : linkID;
      if (!seen.contains(linkID)) {
        Linkage link = genome.getLinkage(linkID);
        retval.add(new ObjChoiceContent(link.getDisplayString(genome, false), linkID));
        seen.add(linkID);
      }
    }
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Build the URL panel
  ** 
  */

  public JPanel buildUrlTab(Set forLinks, boolean forRoot) {
    ResourceManager rMan = appState_.getRMan();    
    GridBagConstraints gbc = new GridBagConstraints();
    
    JPanel urlPanel = new JPanel();    
    urlPanel.setLayout(new GridBagLayout());
    int urlRow = 0;
    
    //
    // Always build it, only use it if needed:
    //
    
    urlListTarget_ = new JComboBox(buildTargetChoices());
    lastUrlTarg_ = ROOT_TARGET_;
    allUrlLists_ = new HashMap();
  
    if (!forRoot) {
      JLabel label = new JLabel(rMan.getString("nprop.urlListTarget"), JLabel.LEFT);
      UiUtil.gbcSet(gbc, 0, urlRow, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.E, 1.0, 0.0);
      urlPanel.add(label, gbc);
      
      urlListTarget_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            manageUrls();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      UiUtil.gbcSet(gbc, 1, urlRow++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.E, 1.0, 0.0);
      urlPanel.add(urlListTarget_, gbc); 
    }
    
    if (forLinks != null) {
      urlsForLinks_ = forLinks;
      JLabel label = new JLabel(rMan.getString("nprop.whichLink"), JLabel.LEFT);
      UiUtil.gbcSet(gbc, 0, urlRow, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.E, 1.0, 0.0);
      urlPanel.add(label, gbc);  
    
      whichUrlLink_ = new JComboBox(buildLinkChoices(urlsForLinks_, true));
      whichUrlLink_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            manageUrlsForLinks();
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      lastUrlLinkTarg_ = ((ObjChoiceContent)whichUrlLink_.getSelectedItem()).val;
      urlListMod_ = false;
      
      
      UiUtil.gbcSet(gbc, 1, urlRow++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.E, 1.0, 0.0);
      urlPanel.add(whichUrlLink_, gbc); 
    }
 
    JPanel textLinePanel = new JPanel();
    textLinePanel.setLayout(new GridBagLayout());        
    
    addEditLabel_ = new JLabel(rMan.getString("nprop.newURL"));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    textLinePanel.add(addEditLabel_, gbc);
    
    urlField_ = new JTextField();
    UiUtil.gbcSet(gbc, 1, 0, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
    textLinePanel.add(urlField_, gbc);
    editing_ = false;
    
    buttonURLAdd_ = new FixedJButton(rMan.getString("dialogs.add"));
    buttonURLAdd_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          addOrEditURLElement();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    UiUtil.gbcSet(gbc, 6, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    textLinePanel.add(buttonURLAdd_, gbc);    
    
    buttonURLCxEdit_ = new FixedJButton(rMan.getString("dialogs.cancel"));
    buttonURLCxEdit_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          cxEditURLElement();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonURLCxEdit_.setEnabled(false);
    
    UiUtil.gbcSet(gbc, 7, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    textLinePanel.add(buttonURLCxEdit_, gbc);        
 
    UiUtil.gbcSet(gbc, 0, urlRow++, 2, 1, UiUtil.HOR, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 0.0);       
    urlPanel.add(textLinePanel, gbc);    
         
    JLabel lab2 = new JLabel(rMan.getString("nprop.URLList"));
    UiUtil.gbcSet(gbc, 0, urlRow++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);       
    urlPanel.add(lab2, gbc);    
    
    urlList_ = new JList();
    urlScroll_ = new JScrollPane(urlList_);
    UiUtil.gbcSet(gbc, 0, urlRow, 2, 10, UiUtil.BO, 0, 0, 0, 0, 0, 0, UiUtil.W, 1.0, 1.0);
    urlRow += 10;
    urlPanel.add(urlScroll_, gbc);
    
    urlList_.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    urlList_.addListSelectionListener(new URLSelectionTracker());
    
    buttonURLDel_ = new FixedJButton(rMan.getString("dialogs.delete"));
    buttonURLDel_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          deleteURLElement();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonURLDel_.setEnabled(false);
    
    buttonURLEdit_ = new FixedJButton(rMan.getString("dialogs.edit"));
    buttonURLEdit_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          editURLElement();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonURLEdit_.setEnabled(false);
  
    buttonURLUp_ = new FixedJButton(rMan.getString("dialogs.raiseEntry"));
    buttonURLUp_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          bumpUpURLElement();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonURLUp_.setEnabled(false);    

    buttonURLDown_ = new FixedJButton(rMan.getString("dialogs.lowerEntry"));
    buttonURLDown_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          bumpDownURLElement();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    buttonURLDown_.setEnabled(false);   
    
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonURLDel_);
    buttonPanel.add(Box.createHorizontalStrut(10));
    buttonPanel.add(buttonURLEdit_);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonURLUp_);
    buttonPanel.add(Box.createHorizontalStrut(10));
    buttonPanel.add(buttonURLDown_);
    buttonPanel.add(Box.createHorizontalGlue());       
    
    UiUtil.gbcSet(gbc, 0, urlRow++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);       
    urlPanel.add(buttonPanel, gbc);    
    
    JLabel labwarn = new JLabel(rMan.getString("nprop.URLwarning"), JLabel.CENTER);
    UiUtil.gbcSet(gbc, 0, urlRow++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);       
    urlPanel.add(labwarn, gbc);  
    
    myURLPanel_ = urlPanel;
    return (urlPanel);
  }
  
  /***************************************************************************
  **
  ** Manage a Url shift
  ** 
  */

  private void manageUrls() {
       
    ChoiceContent targ = (ChoiceContent)urlListTarget_.getSelectedItem();
    if (lastUrlTarg_ == targ.val) {
      return;
    }
    
    //
    // Link case:
    //
    
    if (urlsForLinks_ != null) {
      urlListMod_ = true;
      UiUtil.replaceComboItems(whichUrlLink_, buildLinkChoices(urlsForLinks_, (targ.val == ROOT_TARGET_)));
      urlListMod_ = false;
      lastUrlTarg_ = targ.val;
      whichUrlLink_.setSelectedIndex(0);    
      return;
    }
    
    //
    // node case.  First save what we have:
    //
    
    String useID = (lastUrlTarg_ == ROOT_TARGET_) ? GenomeItemInstance.getBaseID(nodeID_) : nodeID_;
    saveWhatWeHave(useID);
    
    //
    // Now shift the UI to the new value:
    //
    
    useID = (targ.val == ROOT_TARGET_) ? GenomeItemInstance.getBaseID(nodeID_) : nodeID_;   
    ArrayList newList = (ArrayList)allUrlLists_.get(useID);
    urlListData_.clear();
    if (newList != null) {
      urlListData_.addAll(newList);
    }
    urlList_.setListData(urlListData_.toArray());
    urlScroll_.getVerticalScrollBar().setValue(0);
    lastUrlTarg_ = targ.val;
    return;
  }
  
  /***************************************************************************
  **
  ** Save the current UI Url list into backing store
  */

  private void saveWhatWeHave(String useID) {

    ArrayList holdList = (ArrayList)allUrlLists_.get(useID);
    if (holdList == null) {
      holdList = new ArrayList();
      allUrlLists_.put(useID, holdList);
    } else {
      holdList.clear();
    }
    ListModel lmod = urlList_.getModel();
    int unum = lmod.getSize();
    for (int i = 0; i < unum; i++) {
      String url = (String)lmod.getElementAt(i);
      holdList.add(url);
    }
    return;
  }
  
   
  /***************************************************************************
  **
  ** Manage a URL shift for links
  ** 
  */

  private void manageUrlsForLinks() {
    if (urlListMod_) {
      return;
    }
   
    ObjChoiceContent targ = (ObjChoiceContent)whichUrlLink_.getSelectedItem();
    
    // Save current data:
    
    ArrayList holdList = (ArrayList)allUrlLists_.get(lastUrlLinkTarg_);
    if (holdList == null) {
      holdList = new ArrayList();
      allUrlLists_.put(lastUrlLinkTarg_, holdList);
    } else {
      holdList.clear();
    }
    ListModel lmod = urlList_.getModel();
    int unum = lmod.getSize();
    for (int i = 0; i < unum; i++) {
      String url = (String)lmod.getElementAt(i);
      holdList.add(url);
    }
    
    // install new data:
    
    ArrayList newList = (ArrayList)allUrlLists_.get(targ.val);
    urlListData_.clear();
    if (newList != null) {
      urlListData_.addAll(newList);
    }
    urlList_.setListData(urlListData_.toArray());
    urlScroll_.getVerticalScrollBar().setValue(0);
    lastUrlLinkTarg_ = targ.val;
    return;
  }
  
  /***************************************************************************
  **
  ** Move up an element in the URL list
  */
  
  void bumpUpURLElement() {
    if (selectedURLIndex_ <= 0) {
      throw new IllegalStateException();
    }
    calcURL_ = true;
    Object selObj = urlListData_.remove(selectedURLIndex_);   
    urlListData_.add(selectedURLIndex_ - 1, selObj);
    urlList_.setListData(urlListData_.toArray());
    selectedURLIndex_--;
    calcURL_ = false;
    urlList_.getSelectionModel().setSelectionInterval(selectedURLIndex_, 
                                                      selectedURLIndex_);
    myURLPanel_.validate();              
    return;
  }  
  
  /***************************************************************************
  **
  ** Move down an element in the URL list
  */
  
  void bumpDownURLElement() {
    int maxRow = urlList_.getModel().getSize() - 1;   
    if (selectedURLIndex_ >= maxRow) {
      throw new IllegalStateException();
    }
    calcURL_ = true;
    Object selObj = urlListData_.remove(selectedURLIndex_);   
    urlListData_.add(selectedURLIndex_ + 1, selObj);
    urlList_.setListData(urlListData_.toArray());
    selectedURLIndex_++;
    calcURL_ = false;
    urlList_.getSelectionModel().setSelectionInterval(selectedURLIndex_, 
                                                      selectedURLIndex_);
    myURLPanel_.validate();              
    return;
  }
  
  /***************************************************************************
  **
  ** Add a new URL element or finish editing an existing one
  */
  
  void addOrEditURLElement() {
    
    String url = urlField_.getText();
    boolean badURL = (url == null) || url.trim().equals("");
    
    if (!badURL) {
      try {
        URL checkie = new URL(url);
      } catch (MalformedURLException ex) {
        badURL = true;
      }
    }
    
    if (badURL) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                    rMan.getString("nodeProp.badNodeURL"), 
                                    rMan.getString("nodeProp.badNodeURLTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }       
    
    
    calcURL_ = true;
    if (editing_) {
      cxEditURLElement();
      urlListData_.set(selectedURLIndex_, url.trim());
      urlList_.setListData(urlListData_.toArray());
      calcURL_ = false;
      urlList_.getSelectionModel().setSelectionInterval(selectedURLIndex_, 
                                                        selectedURLIndex_);
    } else {
      urlField_.setText("");
      urlListData_.add(url.trim());
      urlList_.setListData(urlListData_.toArray());
      calcURL_ = false;
      urlList_.getSelectionModel().setSelectionInterval(urlListData_.size() - 1, 
                                                        urlListData_.size() - 1);
    }
        
    myURLPanel_.validate();
    return;
  }  
   
  /***************************************************************************
  **
  ** Delete the selected element in the URL list
  */
  
  void deleteURLElement() {
    if (selectedURLIndex_ == -1) {
      throw new IllegalStateException();
    }
    calcURL_ = true;
    urlListData_.remove(selectedURLIndex_);   
    urlList_.setListData(urlListData_.toArray());
    calcURL_ = false;
    int maxRow = urlList_.getModel().getSize() - 1;
    if (maxRow == -1) {
      selectedURLIndex_ = -1;
      buttonURLDel_.setEnabled(false);
      buttonURLEdit_.setEnabled(false);
      buttonURLDown_.setEnabled(false);
      buttonURLUp_.setEnabled(false);
    } else {
      if (maxRow == 0) {
        selectedURLIndex_ = 0;
      } else if (selectedURLIndex_ >= maxRow) {
        selectedURLIndex_ = maxRow;
      }
      urlList_.getSelectionModel().setSelectionInterval(selectedURLIndex_, 
                                                        selectedURLIndex_);     
    }
    myURLPanel_.validate();
    return;
  }

  /***************************************************************************
  **
  ** Cancel Editing the selected element 
  */
  
  void cxEditURLElement() {
    urlField_.setText("");
    editing_ = false;
    ResourceManager rMan = appState_.getRMan(); 
    buttonURLAdd_.setText(rMan.getString("dialogs.add"));
    addEditLabel_.setText(rMan.getString("nprop.newURL"));
    buttonURLCxEdit_.setEnabled(false);
    myURLPanel_.validate();
    return;
  }  
 
  /***************************************************************************
  **
  ** Edit the selected element in the URL list
  */
  
  void editURLElement() {
    if (selectedURLIndex_ == -1) {
      throw new IllegalStateException();
    }
    String selURL = (String)urlListData_.get(selectedURLIndex_);
    urlField_.setText(selURL);
    editing_ = true;
    ResourceManager rMan = appState_.getRMan(); 
    buttonURLAdd_.setText(rMan.getString("nprop.editDone"));
    addEditLabel_.setText(rMan.getString("nprop.editURL"));
    buttonURLCxEdit_.setEnabled(true);
    myURLPanel_.validate();
    return;
  }   
 
  /***************************************************************************
  **
  ** Used to track selected URLs
  */
  
  private class URLSelectionTracker implements ListSelectionListener {
    
    public void valueChanged(ListSelectionEvent lse) {
      try {
        if (lse.getValueIsAdjusting()) {
          return;
        }
        if (calcURL_) {
          return;
        }
              
        int newIndex = urlList_.getSelectedIndex();
        if (editing_ && (newIndex != selectedURLIndex_)) {
          cxEditURLElement();
        }
                
        selectedURLIndex_ = newIndex;
        boolean haveARow = (selectedURLIndex_ != -1);
        buttonURLEdit_.setEnabled(haveARow);          
        buttonURLDel_.setEnabled(haveARow);
        
        int maxRow = urlList_.getModel().getSize() - 1;
        boolean canLower = (haveARow && (selectedURLIndex_ < maxRow));        
        buttonURLDown_.setEnabled(canLower);
        
        boolean canRaise = (haveARow && (selectedURLIndex_ > 0));        
        buttonURLUp_.setEnabled(canRaise);
        myURLPanel_.validate();
      } catch (Exception ex) {
        appState_.getExceptionHandler().displayException(ex);
      }
      return;             
    }  
  }

  /***************************************************************************
  **
  ** Used to display free text for a node
  */
  
  public void displayNodeFreeText() {

    //
    // Have to work with the root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeKey_);
    Genome genome = tooth.getGenome();   
    boolean isInstance = tooth.isInstance();
    
    //
    // Get root if we are not:
    //
    
    if (isInstance) {
      Genome useGenome = dacx_.getDBGenome();
      String useID = GenomeItemInstance.getBaseID(nodeID_);
      Node node = useGenome.getNode(useID);
      String freeText = node.getDescription();
      if (freeText != null) {
        allFreeText_.put(useID, freeText); 
      }
    }
     
    Node node = genome.getNode(nodeID_);
    String freeText = node.getDescription();
    if (freeText != null) {
      allFreeText_.put(nodeID_, freeText); 
    }
    
    ChoiceContent targ = (ChoiceContent)freeTextTarget_.getSelectedItem();
    String useID = (targ.val == ROOT_TARGET_) ? GenomeItemInstance.getBaseID(nodeID_) : nodeID_;   
    freeText = (String)allFreeText_.get(useID); 
    freeTextField_.setText((freeText == null) ? "" : freeText);
    freeTextField_.setCaretPosition(0);
    return;
  }
  
  /***************************************************************************
  **
  ** Used to display free text for links
  */
  
  public void displayLinkFreeText() {
    if (forLinks_ == null) {
      return;
    }
      
    //
    // Have to work with the root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeKey_);
    Genome genome = tooth.getGenome();   
    boolean isInstance = tooth.isInstance();
    
    //
    // Suck up all the descriptions:
    //
    
    HashSet rootSeen = new HashSet();
    Iterator flit = forLinks_.iterator();
    while (flit.hasNext()) {
      String linkID = (String)flit.next();
      if (isInstance) {
        Genome useGenome = dacx_.getDBGenome();
        String useID = GenomeItemInstance.getBaseID(linkID);
        if (!rootSeen.contains(useID)) {
          Linkage link = useGenome.getLinkage(useID);
          String freeText = link.getDescription();
          if (freeText != null) {
            allFreeText_.put(useID, freeText); 
          }
          rootSeen.add(useID);
        }
      }
      Linkage link = genome.getLinkage(linkID);
      String freeText = link.getDescription();
      if (freeText != null) {
        allFreeText_.put(linkID, freeText); 
      }   
    }
    ObjChoiceContent targ = (ObjChoiceContent)whichFreeTextLink_.getSelectedItem();
    String freeText = (String)allFreeText_.get(targ.val); 
    freeTextField_.setText((freeText == null) ? "" : freeText);
    freeTextField_.setCaretPosition(0);
    return;
  }
 
  /***************************************************************************
  **
  ** Used to display URLs
  */
  
  public void displayNodeURLs() {
    //
    // Have to work with the root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeKey_);
    Genome genome = tooth.getGenome();   
    boolean isInstance = tooth.isInstance();

    //
    // Get root if we are not:
    //
    
    if (isInstance) {
      Genome useGenome = dacx_.getDBGenome();
      String useID = GenomeItemInstance.getBaseID(nodeID_);
      Node node = useGenome.getNode(useID);
      Iterator uit = node.getURLs();
      loadURLList(uit, useID);
    }
    
    //
    // Get current node
    //
        
    Node node = genome.getNode(nodeID_);
    Iterator uit = node.getURLs();
    loadURLList(uit, nodeID_);
   
    ChoiceContent targ = (ChoiceContent)urlListTarget_.getSelectedItem();
    String useID = (targ.val == ROOT_TARGET_) ? GenomeItemInstance.getBaseID(nodeID_) : nodeID_;   
    ArrayList forTarg = (ArrayList)allUrlLists_.get(useID); 
    urlListData_ = (forTarg == null) ? new ArrayList() : (ArrayList)forTarg.clone();
    urlList_.setListData(urlListData_.toArray());
    return;
  }  
  
  /***************************************************************************
  **
  ** Used load up urls
  */
  
  private void loadURLList(Iterator uit, String useID) {
    ArrayList newList = new ArrayList();
    allUrlLists_.put(useID, newList);
    while (uit.hasNext()) {
      String url = (String)uit.next();
      newList.add(url);
    }
    return;
  }

  /***************************************************************************
  **
  ** Used to display URLs
  */
  
  public void displayLinkURLs() {
     if (forLinks_ == null) {
       return;
     }
    
    //
    // Have to work with the root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeKey_);
    Genome genome = tooth.getGenome();   
    boolean isInstance = tooth.isInstance();
 
    //
    // Suck up all the url lists:
    //
    
    HashSet rootSeen = new HashSet(); 
    Iterator flit = forLinks_.iterator();
    while (flit.hasNext()) {
      String linkID = (String)flit.next();
      if (isInstance) {
        Genome useGenome = dacx_.getDBGenome();
        String useID = GenomeItemInstance.getBaseID(linkID);
        if (!rootSeen.contains(useID)) {
          Linkage link = useGenome.getLinkage(useID);
          Iterator uit = link.getURLs();
          loadURLList(uit, useID);
          rootSeen.add(useID);
        }
      }
      Linkage link = genome.getLinkage(linkID);
      Iterator uit = link.getURLs();
      loadURLList(uit, linkID);
    }
    
    ObjChoiceContent targ = (ObjChoiceContent)whichUrlLink_.getSelectedItem();
    
    // install new data:
    
    ArrayList newList = (ArrayList)allUrlLists_.get(targ.val);
    urlListData_ = (newList == null) ? new ArrayList() : (ArrayList)newList.clone();
    urlList_.setListData(urlListData_.toArray());
    urlScroll_.getVerticalScrollBar().setValue(0);
    lastUrlLinkTarg_ = targ.val;
    return;
  }  
 
  /***************************************************************************
  **
  ** Used to handle free text install
  */
  
  public boolean installNodeFreeText(UndoSupport support) {
 
    //
    // Have to work with the root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeKey_);
    Genome genome = tooth.getGenome();   
    
    //
    // Make sure we pick up and install the current text first:
    //
    
    ChoiceContent targ = (ChoiceContent)freeTextTarget_.getSelectedItem();
    String useID = (targ.val == ROOT_TARGET_) ? GenomeItemInstance.getBaseID(nodeID_) : nodeID_;
    String currText = freeTextField_.getText();
    allFreeText_.put(useID, currText); 
  
    //
    // Install what is new:
    //
    
    boolean haveSomeNewText = false;
    Iterator aftit = allFreeText_.keySet().iterator();
    while (aftit.hasNext()) {
      String nodeID = (String)aftit.next();
      String freeText = (String)allFreeText_.get(nodeID);
      if ((freeText != null) && freeText.trim().equals("")) {
        freeText = null;
      }
      Genome useGenome = (GenomeItemInstance.isBaseID(nodeID)) ? dacx_.getDBGenome() : genome;
      Node node = useGenome.getNode(nodeID); 
      String currDesc = node.getDescription();
      boolean haveNewText = false;
      if (currDesc == null) {
        haveNewText = (freeText != null);
      } else {
        haveNewText = !currDesc.equals(freeText);
      }
      if (haveNewText) {
        haveSomeNewText = true;
        GenomeChange gc = useGenome.changeNodeDescription(nodeID, freeText);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
          support.addEdit(gcc);
        }
      }
    }
      
    return (haveSomeNewText);
  }
  
  /***************************************************************************
  **
  ** Used to handle URL install
  */
  
  public boolean installNodeURLs(UndoSupport support) {
 
    //
    // Have to work with the root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeKey_);
    Genome genome = tooth.getGenome();   
    boolean isInstance = tooth.isInstance();
    
    //
    // Make sure we pick up and install the current URLs first;
    //    
    
    String useID = (lastUrlTarg_ == ROOT_TARGET_) ? GenomeItemInstance.getBaseID(nodeID_) : nodeID_;
    saveWhatWeHave(useID);
    
    boolean haveNew = false;
    if (isInstance) {
      Genome useGenome = dacx_.getDBGenome();
      useID = GenomeItemInstance.getBaseID(nodeID_);
      Node rNode = useGenome.getNode(useID);      
      haveNew = perNodeUrlInstall(dacx_.getDBGenome(), rNode, useID, support);
    }
    
    Node node = genome.getNode(nodeID_);
    boolean haveNewToo = perNodeUrlInstall(genome, node, nodeID_, support);
    
    return (haveNew || haveNewToo);
  } 
  
  /***************************************************************************
  **
  ** Used to handle URL install for a single node
  */
  
  private boolean perNodeUrlInstall(Genome genome, Node node, String nodeID, UndoSupport support) {
  
    boolean haveNewURLs = false;

    ArrayList urls = (ArrayList)allUrlLists_.get(nodeID);
    int newCount = urls.size();
    if (newCount != node.getURLCount()) {
      haveNewURLs = true;
    } else {
      int count = 0;
      Iterator uit = node.getURLs();
      while (uit.hasNext()) {
        String url = (String)uit.next();
        String myUrl = (String)urls.get(count++);
        if (!url.equals(myUrl)) {
          haveNewURLs = true;
          break;
        }
      }
    }
  
    if (haveNewURLs) {
      GenomeChange gc = genome.changeNodeURLs(nodeID, (List)urls.clone());
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
        support.addEdit(gcc);
        return (true);
      }
    }
    return (false);
  } 
  
  /***************************************************************************
  **
  ** Used to handle free text install
  */
  
  public boolean installLinkFreeText(UndoSupport support) {
   
    //
    // Have to work with the root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeKey_);
    Genome genome = tooth.getGenome();   
    
    //
    // Make sure we pick up and install the current text first:
    //
    
    ObjChoiceContent targ = (ObjChoiceContent)whichFreeTextLink_.getSelectedItem();
    String currText = freeTextField_.getText();
    allFreeText_.put(targ.val, currText); 
 
    //
    // Install what is new:
    //
    
    boolean haveSomeNewText = false;
    Iterator aftit = allFreeText_.keySet().iterator();
    while (aftit.hasNext()) {
      String linkID = (String)aftit.next();
      String freeText = (String)allFreeText_.get(linkID);
      if ((freeText != null) && freeText.trim().equals("")) {
        freeText = null;
      }
      Genome useGenome = (GenomeItemInstance.isBaseID(linkID)) ? dacx_.getDBGenome() : genome;
      Linkage link = useGenome.getLinkage(linkID); 
      String currDesc = link.getDescription();
      boolean haveNewText = false;
      if (currDesc == null) {
        haveNewText = (freeText != null) && !freeText.trim().equals("");
      } else {
        haveNewText = !currDesc.equals(freeText);
      }
      if (haveNewText) {
        haveSomeNewText = true;
        GenomeChange gc = useGenome.changeLinkageDescription(linkID, freeText);
        if (gc != null) {
          GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
          support.addEdit(gcc);
        }
      }
    }
    
    return (haveSomeNewText);
  }
  
  /***************************************************************************
  **
  ** Used to handle URL install
  */
  
  public boolean installLinkURLs(UndoSupport support) {
 
    //
    // Have to work with the root instance:
    //
    
    TopOfTheHeap tooth = new TopOfTheHeap(appState_, genomeKey_);
    Genome genome = tooth.getGenome();
    
    //
    // Make sure we pick up and install the current URLs first;
    //
    
    ObjChoiceContent targ = (ObjChoiceContent)whichUrlLink_.getSelectedItem();
    saveWhatWeHave(targ.val);
    
    boolean haveNewUrls = false;
    Iterator aulit = allUrlLists_.keySet().iterator();
    while (aulit.hasNext()) {
      String linkID = (String)aulit.next();
      Genome useGenome = (GenomeItemInstance.isBaseID(linkID)) ? dacx_.getDBGenome() : genome;
      Linkage link = useGenome.getLinkage(linkID);
      boolean haveNew = perLinkUrlInstall(useGenome, link, linkID, support);
      haveNewUrls = haveNewUrls || haveNew;
    }
  
    return (haveNewUrls);
  }
  
   /***************************************************************************
  **
  ** Used to handle URL install for a single link
  */
  
  private boolean perLinkUrlInstall(Genome genome, Linkage link, String linkID, UndoSupport support) {
  
    boolean haveNewURLs = false;

    ArrayList urls = (ArrayList)allUrlLists_.get(linkID);
    int newCount = urls.size();
    if (newCount != link.getURLCount()) {
      haveNewURLs = true;
    } else {
      int count = 0;
      Iterator uit = link.getURLs();
      while (uit.hasNext()) {
        String url = (String)uit.next();
        String myUrl = (String)urls.get(count++);
        if (!url.equals(myUrl)) {
          haveNewURLs = true;
          break;
        }
      }
    }
  
    if (haveNewURLs) {
      GenomeChange gc = genome.changeLinkageURLs(linkID, (List)urls.clone());
      if (gc != null) {
        GenomeChangeCmd gcc = new GenomeChangeCmd(appState_, dacx_, gc);
        support.addEdit(gcc);
        return (true);
      }
    }
    return (false);
  } 

  /***************************************************************************
  **
  ** Build the direction panel
  */
  
  public JPanel orientUI(Node node) {
    JPanel retval = null;
    int type = node.getNodeType();
    Vector orients = NodeProperties.getOrientTypeChoices(appState_, type);
    if (orients.size() > 1) {
      retval = new JPanel();
      retval.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints(); 
      ResourceManager rMan = appState_.getRMan();
      
      JLabel label = new JLabel(rMan.getString("nprop.orient"));
      orientCombo_ = new JComboBox(orients);      
       
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
      retval.add(label, gbc);
    
      UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
      retval.add(orientCombo_, gbc);
    }
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build the direction panel for multi selections
  */
  
  public JPanel orientUIForMulti(int maxCount, boolean isPartial, ImageIcon warnIcon) {
    JPanel retval = null;
    
    if ((maxCount == 0) || (maxCount == 1)) {
      return (retval);
    }
    ResourceManager rMan = appState_.getRMan();
    
    int useType = (maxCount == 4) ? Node.INTERCELL : Node.GENE;
    Vector orients = NodeProperties.getOrientTypeChoices(appState_, useType);
    orients.add(0, new ChoiceContent(rMan.getString("multiSelProps.various"), NodeProperties.NONE));
    retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    

    JLabel label;
    if (isPartial) {
      label = new JLabel(rMan.getString("nprop.orient"), warnIcon, JLabel.CENTER);
    } else {
      label = new JLabel(rMan.getString("nprop.orient"));
    } 
    
    orientCombo_ = new JComboBox(orients);      

    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    retval.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);
    retval.add(orientCombo_, gbc);
    return (retval);
  }
 
  /***************************************************************************
  **
  ** Display orientation properties
  */
  
  public void orientDisplay(int orientDisplay) {
    if (orientCombo_ != null) {
      orientCombo_.setSelectedItem(NodeProperties.orientTypeForCombo(appState_, orientDisplay));
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Display orientation properties
  */
  
  public void orientDisplayForMulti(int orientDisplay) {
    if (orientCombo_ != null) {
      if (orientDisplay == NodeProperties.NONE) {
        orientCombo_.setSelectedIndex(0);
      } else {
        orientCombo_.setSelectedItem(NodeProperties.orientTypeForCombo(appState_, orientDisplay));
      }
    }
    return;
  }

  /***************************************************************************
  **
  ** Used to return orientation
  */
  
  public boolean haveOrientationForMulti() {
    return (orientCombo_ != null);
  }
  
  /***************************************************************************
  **
  ** Used to return orientation
  */
  
  public int getOrientationForMulti() {
    if (orientCombo_ == null) {
      throw new IllegalStateException();
    }
    return (((ChoiceContent)orientCombo_.getSelectedItem()).val);
  }
  
  /***************************************************************************
  **
  ** Used to return orientation
  */
  
  public int getOrientation() { 
    if (orientCombo_ == null) {
      throw new IllegalStateException();
    }
    return (((ChoiceContent)orientCombo_.getSelectedItem()).val);
  } 
  
  /***************************************************************************
  **
  ** Used to handle font override
  */
  
  public JPanel fontOverrideUI(boolean forMulti) {  
  
    //
    // Build font support
    //
    
    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = appState_.getRMan();
       
    int usedCols = 0;
    if (forMulti) {
      doOverFontMultiCombo_ = new TriStateJComboBox(appState_);      
      doOverFontMultiCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            int multiChoice = doOverFontMultiCombo_.getSetting();
            fsp_.setEnabled(multiChoice == TriStateJComboBox.TRUE);
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      JLabel doMultiLabel = new JLabel(rMan.getString("nprops.overFont"));
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
      retval.add(doMultiLabel, gbc);
      UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
      retval.add(doOverFontMultiCombo_, gbc);
      usedCols = 2;
    } else {
      doOverFontBox_ = new JCheckBox(rMan.getString("nprops.overFont"));      
      doOverFontBox_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            fsp_.setEnabled(doOverFontBox_.isSelected());
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
      retval.add(doOverFontBox_, gbc);
      usedCols = 1;
    }
 
    fsp_ = new FontSettingPanel(appState_);

    UiUtil.gbcSet(gbc, usedCols, 0, 3, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    retval.add(fsp_, gbc);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Display font override properties
  */
  
  public void fontOverrideDisplay(FontManager.FontOverride fo, int baseFont) {
    doOverFontBox_.setSelected(fo != null);
    fsp_.setEnabled(fo != null);  // above callback doesn't fire with no change, right?
    FontManager fmgr = appState_.getFontMgr();
    Font bFont = fmgr.getOverrideFont(baseFont, fo).getFont();
    fsp_.displayProperties(bFont);
    return;
  }
  
  /***************************************************************************
  **
  ** Display font override properties
  */
  
  public void fontOverrideDisplayForMulti(int triState, FontManager.FontOverride fo, int baseFont) {
    doOverFontMultiCombo_.setSetting(triState);
    fsp_.setEnabled(triState == TriStateJComboBox.TRUE); // above callback doesn't fire with no change, right?
    FontManager fmgr = appState_.getFontMgr();
    Font bFont = fmgr.getOverrideFont(baseFont, fo).getFont();
    fsp_.displayProperties(bFont);
    return;
  }

  /***************************************************************************
  **
  ** Used to return font override
  */
  
  public FontManager.FontOverride getFontOverrideForMulti() { 
    return ((doOverFontMultiCombo_.getSetting() == TriStateJComboBox.TRUE) ? fsp_.getFontResult() : null);
  }
  
  /***************************************************************************
  **
  ** Used to return font override
  */
  
  public FontManager.FontOverride getFontOverride() { 
    return ((doOverFontBox_.isSelected()) ? fsp_.getFontResult() : null);
  } 
 
  /***************************************************************************
  **
  ** Used to handle extra pad settings
  */
  
  public JPanel extraPadsUI(boolean forMulti, SortedSet padOptions, boolean isPartial, ImageIcon warnIcon) {
  
    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = appState_.getRMan();
    Vector pads = new Vector(padOptions);
    
    //
    // Big size box and pad setter:
    //
    
    if (forMulti) {
      bigBoxMultiCombo_ = new TriStateJComboBox(appState_);      
      bigBoxMultiCombo_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            int multiChoice = bigBoxMultiCombo_.getSetting();
            extraPadCombo_.setEnabled(multiChoice == TriStateJComboBox.TRUE);
            extraPadLabel_.setEnabled(multiChoice == TriStateJComboBox.TRUE);
            if (extraGrowthCombo_ != null) {
              extraGrowthCombo_.setEnabled(multiChoice == TriStateJComboBox.TRUE);
              extraGrowthLabel_.setEnabled(multiChoice == TriStateJComboBox.TRUE);
            }    
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
          return;
        }
      });
      
      JLabel doMultiLabel;
      if (isPartial) {
        doMultiLabel = new JLabel(rMan.getString("nprop.getBig"), warnIcon, JLabel.CENTER);
      } else {
        doMultiLabel = new JLabel(rMan.getString("nprop.getBig"));
      }
      UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
      retval.add(doMultiLabel, gbc);
      UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 1.0);       
      retval.add(bigBoxMultiCombo_, gbc);
      
    } else {
      bigBox_ = new JCheckBox(rMan.getString("nprop.getBig"));
      UiUtil.gbcSet(gbc, 0, 0, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
      retval.add(bigBox_, gbc);
      bigBox_.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            extraPadCombo_.setEnabled(bigBox_.isSelected());
            extraPadLabel_.setEnabled(bigBox_.isSelected());
            if (extraGrowthCombo_ != null) {
              extraGrowthCombo_.setEnabled(bigBox_.isSelected());
              extraGrowthLabel_.setEnabled(bigBox_.isSelected());
            }
          } catch (Exception ex) {
            appState_.getExceptionHandler().displayException(ex);
          }
        }
      });
    }
      
    extraPadLabel_ = new JLabel(rMan.getString("nprop.getBigPadChoice"));
    extraPadCombo_ = new JComboBox(pads);
    
    UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    retval.add(extraPadLabel_, gbc);

    UiUtil.gbcSet(gbc, 1, 1, 10, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);
    retval.add(extraPadCombo_, gbc);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Used to set extra pads (and extra growth)
  */
  
  public void setExtraPads(int pads, boolean amBig, int extraGrowthDirection) { 
    if (bigBox_ != null) {
      bigBox_.setSelected(amBig);
      extraPadLabel_.setEnabled(amBig);
      extraPadCombo_.setEnabled(amBig);
      extraPadCombo_.setSelectedItem(new Integer(pads));       
      if (extraGrowthLabel_ != null) {
        extraGrowthLabel_.setEnabled(amBig);
        extraGrowthCombo_.setEnabled(amBig);
        extraGrowthCombo_.setSelectedItem(NodeProperties.growthTypeForCombo(appState_, extraGrowthDirection));
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Used to set extra pads (and extra growth)
  */
  
  public void setExtraPadsForMulti(int pads, int consensusBig, int extraGrowthDirection) { 
    if (bigBoxMultiCombo_ != null) {
      bigBoxMultiCombo_.setSetting(consensusBig);
      extraPadLabel_.setEnabled(consensusBig == TriStateJComboBox.TRUE);
      extraPadCombo_.setEnabled(consensusBig == TriStateJComboBox.TRUE);
      extraPadCombo_.setSelectedItem(new Integer(pads));       
      if (extraGrowthLabel_ != null) {
        extraGrowthLabel_.setEnabled(consensusBig == TriStateJComboBox.TRUE);
        extraGrowthCombo_.setEnabled(consensusBig == TriStateJComboBox.TRUE);   
        if (extraGrowthDirection == NodeProperties.NO_GROWTH) {
          extraGrowthCombo_.setSelectedIndex(0);
        } else {
          extraGrowthCombo_.setSelectedItem(NodeProperties.growthTypeForCombo(appState_, extraGrowthDirection));
        }
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Used to get check for extra pad errors
  */
  
  public boolean checkExtraPads(Node node, boolean forMulti) { 
    boolean wantBig;
    if (forMulti) {
      if (bigBoxMultiCombo_ == null) {
        return (true);
      }
      int triVal = bigBoxMultiCombo_.getSetting();
      if (triVal == TriStateJComboBox.MIXED_STATE) {
        return (true);
      }
      wantBig = (triVal == TriStateJComboBox.TRUE);
    } else {
      if (bigBox_ == null) {
        return (true);
      }
      wantBig = bigBox_.isSelected();
    }
      
    int newPads;
    if (wantBig) {
      newPads = ((Integer)extraPadCombo_.getSelectedItem()).intValue();
    } else {
      newPads = DBNode.getDefaultPadCount(node.getNodeType());
    }
    
    FullGenomeHierarchyOracle fgho = new FullGenomeHierarchyOracle(appState_);  
    PadCalculatorToo.PadResult padreq = fgho.getNodePadRequirements(GenomeItemInstance.getBaseID(node.getID()));
               
    if ((padreq.landing > newPads) || (padreq.launch > newPads)) {
      ResourceManager rMan = appState_.getRMan();
      JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                    rMan.getString("nodeProp.needMorePads"), 
                                    rMan.getString("nodeProp.needMorePadsTitle"),
                                    JOptionPane.ERROR_MESSAGE);         
      return (false);            
    }
    return (true);
  }
  
  /***************************************************************************
  **
  ** Answer if we want extra pads.  Returns null if set to multi.
  */
  
  public Boolean wantExtraPadsForMulti() {
    if (bigBoxMultiCombo_ == null) {
      return (null);
    }
    int triVal = bigBoxMultiCombo_.getSetting();
    if (triVal == TriStateJComboBox.MIXED_STATE) {
      return (null);
    }
    return (new Boolean(triVal == TriStateJComboBox.TRUE));  
  }

  /***************************************************************************
  **
  ** Used to get extra pads.  Returns null if default size chosen
  */
  
  public Integer getExtraPadsForMulti() {
    if (bigBoxMultiCombo_ == null) {
      throw new IllegalStateException();
    }
    int triVal = bigBoxMultiCombo_.getSetting();
    if (triVal == TriStateJComboBox.MIXED_STATE) {
      throw new IllegalStateException();
    }
    if (triVal == TriStateJComboBox.TRUE) {
      return ((Integer)extraPadCombo_.getSelectedItem());
    } else {
      return (null);
    }
  }
    
  /***************************************************************************
  **
  ** Used to get extra pads.  Returns null for default size chosen
  */
  
  public Integer getExtraPads() {   
    if (bigBox_ == null) {
      return (null);
    }
    boolean wantBig = bigBox_.isSelected();
    if (!wantBig) {
      return (null);
    }
    Integer newPads = (Integer)extraPadCombo_.getSelectedItem();
    return (newPads);
  }
  
  /***************************************************************************
  **
  ** Used to handle extra growth direction
  */
  
  public JPanel extraGrowthUI(boolean forMulti, int nodeType, boolean isPartial, ImageIcon warnIcon) {
    
    Vector extraG;
    if (!forMulti) {  
      if (!NodeProperties.usesGrowth(nodeType)) {
        return (null);
      }
      extraG = NodeProperties.getExtraGrowthChoices(appState_, nodeType);
      if (extraG.size() <= 1) {
        return (null);
      }
    } else {
      extraG = NodeProperties.getExtraGrowthChoices(appState_, Node.BUBBLE);  // FIX ME; bogus
    }
    
    
    ResourceManager rMan = appState_.getRMan();
    if (forMulti) {
      extraG.add(0, new ChoiceContent(rMan.getString("multiSelProps.various"), NodeProperties.NO_GROWTH));
    }

    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    
    if (isPartial) {
      extraGrowthLabel_ = new JLabel(rMan.getString("nprop.extraGrowth"), warnIcon, JLabel.CENTER);
    } else {
      extraGrowthLabel_ = new JLabel(rMan.getString("nprop.extraGrowth"));
    }
    
    extraGrowthCombo_ = new JComboBox(extraG);

    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    retval.add(extraGrowthLabel_, gbc);

    UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 0.0, 0.0);
    retval.add(extraGrowthCombo_, gbc);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Used to get extra growth direction.  Returns null for not active.
  */
  
  public Integer getExtraGrowth(int nodeType) {
    if (bigBox_ == null) {
      return (null);
    }
    boolean wantBig = bigBox_.isSelected();
    if (!wantBig) {
      return (null);
    }
    if (!NodeProperties.usesGrowth(nodeType)) {
      return (null);
    }
    Integer growth = new Integer(((ChoiceContent)extraGrowthCombo_.getSelectedItem()).val);
    return (growth);
  }
  
  /***************************************************************************
  **
  ** Used to get extra growth direction.  Null if not available
  */
  
  public Integer getExtraGrowthForMulti() {
    
    Boolean wantIt = wantExtraPadsForMulti();
    if ((wantIt == null) || !wantIt.booleanValue()) {
      throw new IllegalStateException();
    }
    if (extraGrowthCombo_ == null) {
      return (null);
    }
    Integer growth = new Integer(((ChoiceContent)extraGrowthCombo_.getSelectedItem()).val);
    return (growth);
  }
  
  /***************************************************************************
  **
  ** Used to handle activity level
  */
  
  public JPanel activityLevelUI(boolean forMulti, boolean forLinks) {
  
    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = appState_.getRMan();
   
    Vector choices;
    int notSet;
    final int variable;
    String labelStr;
    if (forLinks) {
      choices = LinkageInstance.getActivityChoices(appState_);
      notSet = LinkageInstance.ACTIVITY_NOT_SET;
      variable = LinkageInstance.VARIABLE;
      labelStr = rMan.getString("lprop.activity");     
    } else {
      choices = NodeInstance.getActivityChoices(appState_);
      notSet = NodeInstance.ACTIVITY_NOT_SET;
      variable = NodeInstance.VARIABLE;
      labelStr = rMan.getString("nprop.activity");
    }
  
    levelLabel_ = new JLabel(rMan.getString("nprop.activityLevel"));
    activityLevel_ = new JTextField();
    if (forMulti) {
      choices.add(0, new ChoiceContent(rMan.getString("multiSelProps.various"), notSet));
    }
    activityCombo_ = new JComboBox(choices);
    activityCombo_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          int currActivity = ((ChoiceContent)activityCombo_.getSelectedItem()).val; 
          boolean activated = (currActivity == variable);            
          activityLevel_.setEnabled(activated);
          levelLabel_.setEnabled(activated);
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    });      

    JLabel label = new JLabel(labelStr);
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    retval.add(label, gbc);

    UiUtil.gbcSet(gbc, 1, 0, 5, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(activityCombo_, gbc);

    UiUtil.gbcSet(gbc, 6, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    retval.add(levelLabel_, gbc);            

    UiUtil.gbcSet(gbc, 7, 0, 4, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    retval.add(activityLevel_, gbc);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Used to set activity level
  */
  
  public void setActivityLevel(NodeInstance ni) { 
    int activity = ni.getActivity();
    activityCombo_.setSelectedItem(NodeInstance.activityTypeForCombo(appState_, activity));
    if (activity == NodeInstance.VARIABLE) { 
      activityLevel_.setText(Double.toString(ni.getActivityLevel()));
      activityLevel_.setEnabled(true);
      levelLabel_.setEnabled(true);
    } else {
      activityLevel_.setText("");
      activityLevel_.setEnabled(false);
      levelLabel_.setEnabled(false);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Used to set activity level
  */
  
  public void setActivityLevelForMulti(GenomeItemInstance.ActivityState astate, boolean forLink) { 
    if (astate.activityState == GenomeItemInstance.ACTIVITY_NOT_SET) {
      activityCombo_.setSelectedIndex(0);
      activityLevel_.setText("");
      activityLevel_.setEnabled(false);
      levelLabel_.setEnabled(false);
      return;
    }
    
    ChoiceContent scc = (forLink) ? LinkageInstance.activityTypeForCombo(appState_, astate.activityState)
                                  : NodeInstance.activityTypeForCombo(appState_, astate.activityState);
    activityCombo_.setSelectedItem(scc);
    if (astate.activityState == GenomeItemInstance.VARIABLE) { 
      activityLevel_.setText(astate.activityLevel.toString());
      activityLevel_.setEnabled(true);
      levelLabel_.setEnabled(true);
    } else {
      activityLevel_.setText("");
      activityLevel_.setEnabled(false);
      levelLabel_.setEnabled(false);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Used to get activity level
  */
  
  public GenomeItemInstance.ActivityState getActivityLevel() { 
  
    double newLevel = 0.0;
    int newActivity = ((ChoiceContent)activityCombo_.getSelectedItem()).val;
    if (newActivity == GenomeItemInstance.VARIABLE) {
      String newLevelTxt = activityLevel_.getText();
      boolean badNum = false;
      if ((newLevelTxt == null) || newLevelTxt.trim().equals("")) {
        badNum = true;
      } else {
        try {
          newLevel = Double.parseDouble(newLevelTxt);
          if ((newLevel < 0.0) || (newLevel > 1.0)) {
            badNum = true;
          }
        } catch (NumberFormatException nfe) {
          badNum = true;
        }
      }
      if (badNum) {   
        ResourceManager rMan = appState_.getRMan();
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      rMan.getString("nodeProp.badActivityNumber"), 
                                      rMan.getString("nodeProp.badActivityNumberTitle"),
                                      JOptionPane.ERROR_MESSAGE);         
        return (null);            
      }
    }
    
    GenomeItemInstance.ActivityState retval = new GenomeItemInstance.ActivityState(newActivity, newLevel);
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Tracks name changes for line break updates
  */
     
  public void trackNodeNameChanges(JTextField field, JTextField localField) {
    globalCumulativeBreak_ = new BreakState();
    globalCumulativeBreak_.nameField = field;
    globalCumulativeBreak_.lastName = null;
    globalCumulativeBreak_.nameSteps = new LineBreaker.LineBreakChangeSteps();
    
    globalBreak_ = new BreakState();
    globalBreak_.nameField = field;
    field.getDocument().addDocumentListener(this);
    globalBreak_.lastName = null;
    globalBreak_.nameSteps = new LineBreaker.LineBreakChangeSteps();

    if (localField != null) {
      localBreak_ = new BreakState();
      localBreak_.nameField = localField;
      localField.getDocument().addDocumentListener(this);
      localBreak_.lastName = null;
      localBreak_.nameSteps = new LineBreaker.LineBreakChangeSteps();
    }
    return;
  }   

 /***************************************************************************
  **
  ** Call when switching between local and global naming
  */
     
  public void displayLocalForBreaks(boolean doLocal) {
    String currDef = MultiLineRenderSupport.genBreaks(lbp_.getText());
    String broken;
    if (doLocal) {
      globalBreak_.breakDef = currDef;
      broken = MultiLineRenderSupport.applyLineBreaks(localBreak_.lastName, localBreak_.breakDef);
    } else {
      localBreak_.breakDef = currDef;
      broken = MultiLineRenderSupport.applyLineBreaks(globalBreak_.lastName, globalBreak_.breakDef);    
    }
    doLocalBreak_ = doLocal;
    breakDoc_.setBaseText(broken);
    return;
  }  
  
  /***************************************************************************
  **
  ** Get the tracking history
  */
     
  public LineBreaker.LineBreakChangeSteps getNameChangeTracking() {
    return (globalCumulativeBreak_.nameSteps);
  }  
  
  /***************************************************************************
  **
  ** Clear the tracking history
  */
     
  public void clearNameChangeTracking() {
    globalCumulativeBreak_.nameSteps.clearChanges();
    return;
  }    
  
  /***************************************************************************
  **
  ** Used to define line breaks
  */
  
  public JPanel getLineBreakUI() { 

    JPanel retval = new JPanel();
    retval.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints(); 
    ResourceManager rMan = appState_.getRMan();
       
    doMultiLine_ = new JCheckBox(rMan.getString("nprops.makeMultiLine"));      
    doMultiLine_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          lbp_.setEnabled(doMultiLine_.isSelected());
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    });
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);       
    retval.add(doMultiLine_, gbc);    
 
    breakDoc_ = new NewLineOnlyDocument(this);
    lbp_ = new JTextArea(breakDoc_);
    lbp_.setEditable(true);
    JScrollPane jsp = new JScrollPane(lbp_); 
  
    UiUtil.gbcSet(gbc, 0, 1, 1, 5, UiUtil.BO, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    retval.add(jsp, gbc);
    return (retval);    
  }

  /***************************************************************************
  **
  ** Set the line break def.  UI controls set consistently.  text setting
  ** arrives from cursor listener
  */
  
  public void setLineBreak(String breakDef, boolean fromLocal) {
    doLocalBreak_ = fromLocal;
    doMultiLine_.setSelected(breakDef != null);
    lbp_.setEnabled(breakDef != null);
    if (fromLocal) {
      localBreak_.breakDef = breakDef;
      localBreak_.origBreakDef = breakDef;
    } else {
      globalBreak_.breakDef = breakDef;
      globalBreak_.origBreakDef = breakDef;
      globalCumulativeBreak_.breakDef = breakDef;
      globalCumulativeBreak_.origBreakDef = breakDef;      
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** When we edit the line break state, reset it, since the line break
  ** definition modifications are all run in a cumulative fashion
  */
  
  public void resetLineBreak() {
    String breakDef = MultiLineRenderSupport.genBreaks(lbp_.getText());
    if (doLocalBreak_) {
      localBreak_.breakDef = breakDef;
      localBreak_.origBreakDef = breakDef;
      localBreak_.nameSteps.clearChanges();
    } else {
      globalBreak_.breakDef = breakDef;
      globalBreak_.origBreakDef = breakDef;
      globalBreak_.nameSteps.clearChanges();
    }
    return;
  }   
  
  /***************************************************************************
  **
  ** Get the line break state
  */
  
  public String getLineBreakDef() {
    if (!doMultiLine_.isSelected()) {
      return (null);
    }
    String breakDef = MultiLineRenderSupport.genBreaks(lbp_.getText());
    return (breakDef);
  }
  
  /***************************************************************************
  **
  ** Get the base string corresponding to the break def
  */
  
  public String getBaseStringForDef() {
    return (MultiLineRenderSupport.stripBreaks(lbp_.getText()));
  }  
  
  /***************************************************************************
  **
  ** For document listening
  */
  
  public void insertUpdate(DocumentEvent e) {
    try {
      if (e.getDocument() == globalBreak_.nameField.getDocument()) {
        processChange(globalBreak_.nameField, globalBreak_, e, LineBreaker.ChangeStep.INSERT, !doLocalBreak_);
        processChange(globalCumulativeBreak_.nameField, globalCumulativeBreak_, e, LineBreaker.ChangeStep.INSERT, false);
      } else {
        processChange(localBreak_.nameField, localBreak_, e, LineBreaker.ChangeStep.INSERT, doLocalBreak_);
      }
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }

  /***************************************************************************
  **
  ** For document listening
  */
  
  public void removeUpdate(DocumentEvent e) {
    try {
      if (e.getDocument() == globalBreak_.nameField.getDocument()) {
        processChange(globalBreak_.nameField, globalBreak_, e, LineBreaker.ChangeStep.DELETE, !doLocalBreak_);
        processChange(globalCumulativeBreak_.nameField, globalCumulativeBreak_, e, LineBreaker.ChangeStep.DELETE, false);      
      } else {
        processChange(localBreak_.nameField, localBreak_, e, LineBreaker.ChangeStep.DELETE, doLocalBreak_);
      }
    } catch (Exception ex) {
      appState_.getExceptionHandler().displayException(ex);
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Tracks name changes for line break updates.
  */
     
  private void processChange(JTextField textField, BreakState breakData, 
                             DocumentEvent e, int type, boolean passToUI) {

    String newName = textField.getText();  
    if (!newName.equals(breakData.lastName)) {
      LineBreaker.ChangeStep nextStep = new LineBreaker.ChangeStep(type, e.getOffset(), e.getLength(), newName, breakData.lastName);      
      breakData.nameSteps.addChange(nextStep);
      breakData.breakDef = LineBreaker.resolveNameChange(breakData.origBreakDef, breakData.nameSteps);
      breakData.lastName = newName;
      if (passToUI) {
        String broken = MultiLineRenderSupport.applyLineBreaks(newName, breakData.breakDef);    
        breakDoc_.setBaseText(broken);
      }
    }             
    return;
  }    
   
  /***************************************************************************
  **
  ** For document listening
  */ 

  public void changedUpdate(DocumentEvent e) {
    // This is for tracking attribute changes, which we ignore
    return;
  } 
  
  /***************************************************************************
  **
  ** Generate pad options
  */
  
  public static SortedSet<Integer> generatePadChoices(Node node) {
  
    TreeSet<Integer> pads = new TreeSet<Integer>();  
    int nodeType = node.getNodeType();        
    int padInc = DBNode.getPadIncrement(nodeType);
    if (padInc == 0) {
      return (null);
    }       
    int padCount = node.getPadCount();
    int minPad = DBNode.getDefaultPadCount(nodeType);
    int maxPad = DBNode.getMaxPadCount(nodeType);
    // Autolayout algorithms can blow away the UI-offered maximum pad count:
    maxPad = (padCount > maxPad) ? padCount + 10 : maxPad;      
    for (int i = minPad; i <= maxPad; i += padInc) {
      pads.add(new Integer(i));
    }
    return (pads);
  }
  

  /***************************************************************************
  **
  ** A class for tracking line break state
  */

  private static class BreakState {
    JTextField nameField;  
    String lastName;   
    LineBreaker.LineBreakChangeSteps nameSteps;
    String breakDef;
    String origBreakDef;
  }  
  
  /***************************************************************************
  **
  ** A document that only allows newline insertions and deletions.
  */
  
  static class NewLineOnlyDocument extends PlainDocument {
 
    private NodeAndLinkPropertiesSupport nps;
    
    NewLineOnlyDocument(NodeAndLinkPropertiesSupport nps) {
      this.nps = nps;
    }
    
    void setBaseText(String brokenText) {
      try {
        int len = getLength();
        if (len > 0) {
          super.remove(0, len);
        }
        super.insertString(0, brokenText, null);
      } catch (BadLocationException foo) {
        throw new IllegalStateException();
      }
      return;
    }
   
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
      if (str.equals("\n")) {
        super.insertString(offs, str, a);      
        nps.resetLineBreak();
      }
      return;
    }
  
    public void remove(int offs, int len) throws BadLocationException {
      if (getText(offs, len).equals("\n")) {
        super.remove(offs, len);
        nps.resetLineBreak();
      }
      return;
    }

    public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
      // This is being used always by the JTextArea, even for straight inserts!  Allow
      // it just for that case:
      if (length == 0) {
        super.replace(offset, length, text, attrs);
      }
      return;
    }
  }
  
}
