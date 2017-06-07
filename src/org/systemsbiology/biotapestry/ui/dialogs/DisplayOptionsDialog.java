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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.StaticDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.undo.DisplayOptionsChangeCmd;
import org.systemsbiology.biotapestry.event.GeneralChangeEvent;
import org.systemsbiology.biotapestry.event.LayoutChangeEvent;
import org.systemsbiology.biotapestry.perturb.PerturbationData;
import org.systemsbiology.biotapestry.ui.CustomEvidenceDrawStyle;
import org.systemsbiology.biotapestry.ui.DisplayOptions;
import org.systemsbiology.biotapestry.ui.DisplayOptionsChange;
import org.systemsbiology.biotapestry.ui.MinimalDispOptMgr;
import org.systemsbiology.biotapestry.util.BrightnessField;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.MinMax;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;
import org.systemsbiology.biotapestry.util.UndoSupport;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;

/****************************************************************************
**
** Dialog box for editing font properties
*/

public class DisplayOptionsDialog extends JDialog {

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
  
  private JComboBox busBranchChoice_; 
  private JComboBox evidenceChoice_;  
  private JComboBox firstZoomChoice_; 
  private JComboBox navZoomChoice_;    
  private JComboBox nodeActivityChoice_;    
  private JComboBox linkActivityChoice_;
  private JCheckBox showTreeBox_;
  private FixedJButton buttonCU_;
  private JTextField bigFootField_;
  private JTextField weakLevelField_;
  private BrightnessField inactiveGray_;
  private JFrame parent_;
  private String layoutKey_;
  private SortedMap<Integer, CustomEvidenceDrawStyle> customEvidence_;
  private ArrayList<MinMax> currentColumns_;
  private Map<String, String> currentMeasureColors_;
  private String currentScaleString_;
  private boolean currentInvestMode_;
  private MinMax currentNullDefaultSpan_;
  private StaticDataAccessContext dacx_;
  private UIComponentSource uics_;
  private UndoFactory uFac_;
  private HarnessBuilder hBld_;
  
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
  
  public DisplayOptionsDialog(UIComponentSource uics, StaticDataAccessContext dacx, HarnessBuilder hBld, UndoFactory uFac) {     
    super(uics.getTopFrame(), dacx.getRMan().getString("displayOptions.title"), true);   
    
    uics_ = uics;
    dacx_ = dacx;
    uFac_ = uFac;
    hBld_ = hBld;
    layoutKey_ = dacx_.getCurrentLayoutID();
    ResourceManager rMan = dacx_.getRMan();
    DisplayOptions options = dacx_.getDisplayOptsSource().getDisplayOptions();
    customEvidence_ = new TreeMap<Integer, CustomEvidenceDrawStyle>();
    options.fillCustomEvidenceMap(customEvidence_);
    
    currentColumns_ = new ArrayList<MinMax>();
    Iterator<MinMax> colIt = options.getColumnIterator();
    while (colIt.hasNext()) {
      MinMax nextCol = colIt.next();
      currentColumns_.add(nextCol.clone());   
    }
    currentMeasureColors_ = new HashMap<String, String>(options.getMeasurementDisplayColors());
    currentScaleString_ = options.getPerturbDataDisplayScaleKey();
    currentInvestMode_ = options.breakOutInvestigators();
    currentNullDefaultSpan_ = options.getNullPertDefaultSpan();
     
    setSize(600, 600);
    JPanel cp = (JPanel)getContentPane();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();    
          
    int rowNum = 0;
    
    busBranchChoice_ = new JComboBox(DisplayOptions.branchOptions(dacx_));
    busBranchChoice_.setSelectedItem(DisplayOptions.mapBranchOptions(dacx_, options.getBranchMode()));
    JLabel label = new JLabel(rMan.getString("displayOptions.branchOptions"));
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);    
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(busBranchChoice_, gbc);
    
    evidenceChoice_ = new JComboBox(DisplayOptions.evidenceOptions(dacx_));
    evidenceChoice_.setSelectedItem(DisplayOptions.mapEvidenceOptions(dacx_, options.getEvidence()));
    label = new JLabel(rMan.getString("displayOptions.evidenceOptions"));
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);    
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(evidenceChoice_, gbc);
    evidenceChoice_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          enableCustomEvidence();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
        return;
      }
    });
 
    buttonCU_ = new FixedJButton(rMan.getString("displayOptions.custom"));
    buttonCU_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          CustomEvidenceDialog dialog = new CustomEvidenceDialog(uics_, dacx_, hBld_, customEvidence_);
          dialog.setVisible(true);
          if (dialog.haveResult()) {
            customEvidence_ = dialog.getNewEvidenceMap();      
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 0.0);
    cp.add(buttonCU_, gbc);
    enableCustomEvidence();  
    
    firstZoomChoice_ = new JComboBox(DisplayOptions.getFirstZoomChoices(dacx_));
    firstZoomChoice_.setSelectedItem(DisplayOptions.firstZoomForCombo(dacx_, options.getFirstZoomMode()));
    label = new JLabel(rMan.getString("displayOptions.firstZoom"));
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);    
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(firstZoomChoice_, gbc);
    
    navZoomChoice_ = new JComboBox(DisplayOptions.getNavZoomChoices(dacx_));
    navZoomChoice_.setSelectedItem(DisplayOptions.navZoomForCombo(dacx_, options.getNavZoomMode()));
    label = new JLabel(rMan.getString("displayOptions.navZoom"));
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);    
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(navZoomChoice_, gbc); 
    
    nodeActivityChoice_ = new JComboBox(DisplayOptions.getNodeActivityChoices(dacx_));
    nodeActivityChoice_.setSelectedItem(DisplayOptions.nodeActivityForCombo(dacx_, options.getNodeActivity()));
    label = new JLabel(rMan.getString("displayOptions.nodeActivity"));
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);    
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(nodeActivityChoice_, gbc);
    
    linkActivityChoice_ = new JComboBox(DisplayOptions.getLinkActivityChoices(dacx_));
    linkActivityChoice_.setSelectedItem(DisplayOptions.linkActivityForCombo(dacx_, options.getLinkActivity()));
    label = new JLabel(rMan.getString("displayOptions.linkActivity"));
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);    
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(linkActivityChoice_, gbc);    

    showTreeBox_ = new JCheckBox(rMan.getString("displayOptions.showTree"));
    UiUtil.gbcSet(gbc, 0, rowNum++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(showTreeBox_, gbc);
    showTreeBox_.setSelected(options.showExpressionTableTree());
       
    inactiveGray_ = new BrightnessField(uics_.getHandlerAndManagerSource(), "displayOptions.inactiveGray", options.getInactiveBright(), 
                                        DisplayOptions.INACTIVE_BRIGHT_MIN, DisplayOptions.INACTIVE_BRIGHT_MAX); 
    UiUtil.gbcSet(gbc, 0, rowNum++, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 1.0);
    cp.add(inactiveGray_, gbc);
    
    weakLevelField_ = new JTextField(Double.toString(options.getWeakExpressionLevel()));
    label = new JLabel(rMan.getString("displayOptions.weakExpressionLevel"));
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);    
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(weakLevelField_, gbc);    
    
    bigFootField_ = new JTextField(Integer.toString(options.getExtraFootSize()));
    label = new JLabel(rMan.getString("displayOptions.extraFootSize"));
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);    
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(bigFootField_, gbc);
    
    label = new JLabel(rMan.getString("displayOptions.extraFootWarning"));
    UiUtil.gbcSet(gbc, 0, rowNum++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 1.0);       
    cp.add(label, gbc);
    
    FixedJButton buttonQPCR = new FixedJButton(rMan.getString("displayOptions.qpcrTableSetup"));
    buttonQPCR.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          QpcrSetupDialog npd = QpcrSetupDialog.qpcrSetupDialogWrapper(uics_,
                                                                       dacx_,
                                                                       currentColumns_, 
                                                                       currentMeasureColors_, 
                                                                       currentScaleString_,
                                                                       currentInvestMode_, 
                                                                       currentNullDefaultSpan_,
                                                                       uFac_);
          if (npd == null) {
            return;
          }
          npd.setVisible(true);
          if (!npd.haveResult()) {
            return;
          }
          currentColumns_ = new ArrayList(npd.getColumnResults());         
          currentInvestMode_ = npd.getInvestModeResults();
          currentNullDefaultSpan_ = npd.getSpanResult();  
          currentScaleString_ = npd.getScaleResult(); 
          currentMeasureColors_ = npd.getColorResults();       
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    UiUtil.gbcSet(gbc, 0, rowNum++, 2, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 0.0, 0.0);
    cp.add(buttonQPCR, gbc);
     
    //
    // Build the button panel:
    //

    FixedJButton buttonR = new FixedJButton(rMan.getString("dialogs.resetDefaults"));
    buttonR.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          resetDefaults();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });    
    
    FixedJButton buttonO = new FixedJButton(rMan.getString("dialogs.ok"));
    buttonO.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          if (applyProperties()) {
            DisplayOptionsDialog.this.setVisible(false);
            DisplayOptionsDialog.this.dispose();
          }
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });  
    FixedJButton buttonC = new FixedJButton(rMan.getString("dialogs.close"));
    buttonC.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {    
          DisplayOptionsDialog.this.setVisible(false);
          DisplayOptionsDialog.this.dispose();
        } catch (Exception ex) {
          uics_.getExceptionHandler().displayException(ex);
        }
      }
    });
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonR);    
    buttonPanel.add(Box.createHorizontalGlue()); 
    buttonPanel.add(buttonO);
    buttonPanel.add(Box.createHorizontalStrut(10));    
    buttonPanel.add(buttonC);

    //
    // Build the dialog:
    //
    UiUtil.gbcSet(gbc, 0, rowNum, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.SE, 1.0, 0.0);
    cp.add(buttonPanel, gbc);
    setLocationRelativeTo(uics_.getTopFrame());
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

  /***************************************************************************
  **
  ** Enable the custom evidence dialog
  ** 
  */
  
  private void enableCustomEvidence() { 
    ChoiceContent evidenceChoice = (ChoiceContent)evidenceChoice_.getSelectedItem();
    boolean isCustom = (evidenceChoice.val == DisplayOptions.EVIDENCE_IS_CUSTOM);
    buttonCU_.setEnabled(isCustom);
    return;
  }  
    
  /***************************************************************************
  **
  ** Apply our UI values to the options
  ** 
  */
  
  private boolean applyProperties() { 
    
    //
    // Undo/Redo support
    //
    
    ResourceManager rMan = dacx_.getRMan();
    UndoSupport support = uFac_.provideUndoSupport("undo.displayOptions", dacx_);     

    MinimalDispOptMgr dopmgr = dacx_.getDisplayOptsSource();
    DisplayOptions newOpts = new DisplayOptions(dacx_);
    
    String bigFootStr = bigFootField_.getText();
    if (!bigFootStr.trim().equals("")) {
      int bigFootVal = 0;
      boolean badVal = false;
      try {
        bigFootVal = Integer.parseInt(bigFootStr);
        if (bigFootVal < 0) {
          badVal = true;
        }
      } catch (NumberFormatException ex) {
        badVal = true;
      }
      if (badVal) {
        JOptionPane.showMessageDialog(parent_, 
                                      rMan.getString("displayOptions.badFoot"), 
                                      rMan.getString("displayOptions.badFootTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        return (false);
      }
      newOpts.setExtraFootSize(bigFootVal);
    } else {
      newOpts.setExtraFootSize(0);
    }
   
    if (inactiveGray_.haveResults()) {
      newOpts.setInactiveBright(inactiveGray_.getResults());
    } else {
      String msg = MessageFormat.format(rMan.getString("displayOptions.badInactiveGray"),    
                                           new Object[] {Double.valueOf(DisplayOptions.INACTIVE_BRIGHT_MIN),
                                                         Double.valueOf(DisplayOptions.INACTIVE_BRIGHT_MAX)});     
      JOptionPane.showMessageDialog(parent_, 
                                    msg,
                                    rMan.getString("displayOptions.badInactiveGrayTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (false);     
    }

    String weakLevelStr = weakLevelField_.getText();
    boolean badWeakVal = false;
    double weakLevelVal = 1.0;
    if ((weakLevelStr == null) || weakLevelStr.trim().equals("")) {
      badWeakVal = true;
    } else {
      try {
        weakLevelVal = Double.parseDouble(weakLevelStr);
        if ((weakLevelVal < 0.0) || (weakLevelVal > 1.0)) {
          badWeakVal = true;
        }
      } catch (NumberFormatException ex) {
        badWeakVal = true;
      }

    }
    if (badWeakVal) {
      JOptionPane.showMessageDialog(parent_, 
                                    rMan.getString("displayOptions.badWeakLevel"), 
                                    rMan.getString("displayOptions.badWeakLevelTitle"),
                                    JOptionPane.ERROR_MESSAGE);
      return (false);
    } else {
      newOpts.setWeakExpressionLevel(weakLevelVal);
    }
    
    newOpts.setBranchMode(((ChoiceContent)busBranchChoice_.getSelectedItem()).val);
    newOpts.setEvidence(((ChoiceContent)evidenceChoice_.getSelectedItem()).val);  
    newOpts.setFirstZoomMode(((EnumChoiceContent<DisplayOptions.FirstZoom>)firstZoomChoice_.getSelectedItem()).val);
    newOpts.setNavZoomMode(((EnumChoiceContent<DisplayOptions.NavZoom>)navZoomChoice_.getSelectedItem()).val); 
    newOpts.setNodeActivity(((ChoiceContent)nodeActivityChoice_.getSelectedItem()).val); 
    newOpts.setLinkActivity(((ChoiceContent)linkActivityChoice_.getSelectedItem()).val);
    
    boolean isCustom = (newOpts.getEvidence() == DisplayOptions.EVIDENCE_IS_CUSTOM);
    if (isCustom) {
      Iterator<Integer> ceit = customEvidence_.keySet().iterator();
      while (ceit.hasNext()) {
        Integer level = ceit.next();
        CustomEvidenceDrawStyle ceds = customEvidence_.get(level);
        newOpts.setCustomEvidence(level.intValue(), ceds.clone());
      }
    }
    
    newOpts.setShowExpressionTableTree(showTreeBox_.isSelected()); 
    newOpts.setColumns(currentColumns_);         
    newOpts.setBreakOutInvestigatorMode(currentInvestMode_);
    newOpts.setNullDefaultSpan(currentNullDefaultSpan_);  
    newOpts.setPerturbDataDisplayScaleKey(currentScaleString_); 
    newOpts.setMeasurementDisplayColors(currentMeasureColors_);       
  
    DisplayOptionsChange doc = dopmgr.setDisplayOptions(newOpts);
    PerturbationData pd = dacx_.getExpDataSrc().getPertData();
    pd.dropCachedDisplayState();

    DisplayOptionsChangeCmd docc = new DisplayOptionsChangeCmd(dacx_, doc);
    support.addEdit(docc);

    LayoutChangeEvent lcev = new LayoutChangeEvent(layoutKey_, LayoutChangeEvent.UNSPECIFIED_CHANGE);
    support.addEvent(lcev);
 
    //
    // Gets dynamic proxy caches flushed (they care about weak expression level):
    support.addEvent(new GeneralChangeEvent(GeneralChangeEvent.UNSPECIFIED_CHANGE));        
    
    //
    // Finish undo support:
    //
            
    support.finish();
    return (true);
  }
  
 
  /***************************************************************************
  **
  ** Reset to default values
  ** 
  */
  
  private void resetDefaults() {
    DisplayOptions defOptions = new DisplayOptions(dacx_);
    busBranchChoice_.setSelectedItem(DisplayOptions.mapBranchOptions(dacx_, defOptions.getBranchMode()));
    evidenceChoice_.setSelectedItem(DisplayOptions.mapEvidenceOptions(dacx_, defOptions.getEvidence()));
    firstZoomChoice_.setSelectedItem(DisplayOptions.firstZoomForCombo(dacx_, defOptions.getFirstZoomMode()));
    navZoomChoice_.setSelectedItem(DisplayOptions.navZoomForCombo(dacx_, defOptions.getNavZoomMode()));
    nodeActivityChoice_.setSelectedItem(DisplayOptions.nodeActivityForCombo(dacx_, defOptions.getNodeActivity()));
    linkActivityChoice_.setSelectedItem(DisplayOptions.linkActivityForCombo(dacx_, defOptions.getLinkActivity()));
    inactiveGray_.resetValue(defOptions.getInactiveBright());
    
    bigFootField_.setText(Integer.toString(defOptions.getExtraFootSize()));   
    weakLevelField_.setText(Double.toString(defOptions.getWeakExpressionLevel()));
    showTreeBox_.setSelected(defOptions.showExpressionTableTree());
    customEvidence_.clear();
    return;
  }   
}
