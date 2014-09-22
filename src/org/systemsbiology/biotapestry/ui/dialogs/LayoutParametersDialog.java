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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.ui.LayoutOptions;
import org.systemsbiology.biotapestry.ui.LayoutOptionsManager;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.ui.dialogs.utils.BTStashResultsDialog;
import org.systemsbiology.biotapestry.ui.layouts.HaloLayout;
import org.systemsbiology.biotapestry.ui.layouts.HaloLayoutSetupPanel;
import org.systemsbiology.biotapestry.ui.layouts.StackedBlockLayout;
import org.systemsbiology.biotapestry.ui.layouts.StackedBlockLayoutSetupPanel;
import org.systemsbiology.biotapestry.ui.layouts.WorksheetLayout;
import org.systemsbiology.biotapestry.ui.layouts.WorksheetLayoutSetupPanel;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.FixedJButton;
import org.systemsbiology.biotapestry.util.LinkPlacementGrid;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Dialog box for editing layout options
*/

public class LayoutParametersDialog extends BTStashResultsDialog {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private static final int CROSSING_COEFF_MIN_        = 0;
  private static final int CROSSING_COEFF_MAX_        = 100;
  private static final double CROSSING_COEFF_CONVERT_ = 0.01;
  
  private static final int CROSSING_MULT_MIN_         = 1;
  private static final int CROSSING_MULT_MAX_         = 100;
  private static final double CROSSING_MULT_CONVERT_  = 0.01;
  
  private static final int DIFF_COEFF_MIN_            = 0;
  private static final int DIFF_COEFF_MAX_            = 100;
  private static final double DIFF_COEFF_CONVERT_     = 0.01;
  
  private static final int DIFF_SIGMA_MIN_            = 1;
  private static final int DIFF_SIGMA_MAX_            = 100; 
 
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  private JSlider crossingCoeff_;
  private JSlider crossingMultiplier_;
  private JSlider differenceCoeff_;
  private JSlider differenceSigma_;  
  private int myCrossingCoeff_;
  private int myCrossingMultiplier_;
  private int myDifferenceCoeff_;
  private int myDifferenceSigma_;    
  private JRadioButton firstPassButton_;
  private JRadioButton topoCompressButton_;    
  private JComboBox layerMethodChoice_;    
  private JComboBox maxPerLayerChoice_; 
  private JComboBox optNumberChoice_;    
  private JRadioButton doCrossingReductionButton_;
  private JRadioButton normalizeRowsButton_;
  private JRadioButton incrementalCompressButton_;  
  private JRadioButton inheritanceSquashButton_;
  private JComboBox overlayOptionCombo_;
  private boolean myFirstPass_;
  private boolean myTopoCompress_;
  private int myLayerMethod_;
  private int myMaxPerLayer_;
  private boolean myDoCrossingReduction_;
  private boolean myNormalizeRows_;  
  private boolean myIncrementalCompress_; 
  private boolean myInheritanceSquash_;   
  private int myOverlayOption_;     
  private int myOptNumber_;
  
  private HaloLayoutSetupPanel haloParamPanel_;
  private WorksheetLayoutSetupPanel worksheetParamPanel_;
  private WorksheetLayoutSetupPanel worksheetDiagParamPanel_;
  private StackedBlockLayoutSetupPanel stackedParamPanel_;
  
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
  
  public LayoutParametersDialog(BTState appState) {
    super(appState, "layoutParam.title", new Dimension(950, 550), 1);
    LayoutOptionsManager lopmgr = appState_.getLayoutOptMgr();
    LayoutOptions options = lopmgr.getLayoutOptions();
        
    LinkPlacementGrid.GoodnessParams params = options.goodness;
    ResourceManager rMan = appState_.getRMan();
    myCrossingCoeff_ = (int)Math.round(params.crossingCoeff / CROSSING_COEFF_CONVERT_);
    myCrossingMultiplier_ = (int)Math.round(params.crossingMultiplier / CROSSING_MULT_CONVERT_);
    myDifferenceCoeff_ = (int)Math.round(params.differenceCoeff / DIFF_COEFF_CONVERT_);
    myDifferenceSigma_ = (int)Math.round(params.differenceSigma);    
  
    JTabbedPane tabPane = new JTabbedPane();
    tabPane.addTab(rMan.getString("layoutParam.freshLayout"), buildFreshLayoutTab(lopmgr));    
    tabPane.addTab(rMan.getString("layoutParam.incrementLayout"), buildIncrementalTab(options));
    tabPane.addTab(rMan.getString("layoutParam.linksLayout"), buildLinksTab(options)); 
    tabPane.addTab(rMan.getString("layoutParam.propagateLayout"), buildPropagateTab(options));     

    FixedJButton buttonR = new FixedJButton(rMan.getString("dialogs.resetAllTabsDefaults"));
    buttonR.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          resetDefaults();
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });    

    addTable(tabPane, 8); 
    finishConstructionWithExtraLeftButton(buttonR);
  }
  
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  public LayoutOptions getOptions() {
    return (getLegacyFieldsOptions());
  }
  
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  public StackedBlockLayout.StackedBlockLayoutParams getStackedBlockParams() {
    return ((StackedBlockLayout.StackedBlockLayoutParams)stackedParamPanel_.getParams());
  }
  
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  public WorksheetLayout.WorksheetLayoutParams getWorksheetParams() {
    return ((WorksheetLayout.WorksheetLayoutParams)worksheetParamPanel_.getParams());
  }
  
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  public WorksheetLayout.WorksheetLayoutParams getDiagonalParams() {
    return ((WorksheetLayout.WorksheetLayoutParams)worksheetDiagParamPanel_.getParams());
  }
  
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  public HaloLayout.HaloLayoutParams getHaloParams() {
    return ((HaloLayout.HaloLayoutParams)haloParamPanel_.getParams());
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
  ** Build tab for incremental layout 
  */ 
  
  private JPanel buildIncrementalTab(LayoutOptions options) { 
    ResourceManager rMan = appState_.getRMan();
    JPanel cp = new JPanel();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    firstPassButton_ = new JRadioButton(rMan.getString("layoutParam.firstPass"), options.firstPass);
    UiUtil.gbcSet(gbc, 0, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(firstPassButton_, gbc); 
    topoCompressButton_ = new JRadioButton(rMan.getString("layoutParam.topoCompress"), options.topoCompress);
    UiUtil.gbcSet(gbc, 0, 1, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(topoCompressButton_, gbc);
    
    doCrossingReductionButton_ = new JRadioButton(rMan.getString("layoutParam.crossReduct"), options.doCrossingReduction);
    UiUtil.gbcSet(gbc, 0, 2, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(doCrossingReductionButton_, gbc); 
    normalizeRowsButton_ = new JRadioButton(rMan.getString("layoutParam.normRows"), options.normalizeRows);     
    UiUtil.gbcSet(gbc, 0, 3, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(normalizeRowsButton_, gbc);  
    
    incrementalCompressButton_ = new JRadioButton(rMan.getString("layoutParam.incCompress"), options.incrementalCompress);     
    UiUtil.gbcSet(gbc, 0, 4, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(incrementalCompressButton_, gbc);
          
    layerMethodChoice_ = new JComboBox(LayoutOptions.layerOptions(appState_));
    layerMethodChoice_.setSelectedItem(LayoutOptions.mapLayerOptions(appState_, options.layeringMethod));
    JLabel label = new JLabel(rMan.getString("layoutParam.layoutOptions"));
    UiUtil.gbcSet(gbc, 0, 5, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);    
    UiUtil.gbcSet(gbc, 1, 5, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(layerMethodChoice_, gbc);
    Vector<Integer> choices = new Vector<Integer>();
    for (int i = 5; i < 41; i++) {
      choices.add(new Integer(i));
    }
    maxPerLayerChoice_ = new JComboBox(choices);
    maxPerLayerChoice_.setSelectedItem(new Integer(options.maxPerLayer));
    label = new JLabel(rMan.getString("layoutParam.maxPerLayer"));
    UiUtil.gbcSet(gbc, 0, 6, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);    
    UiUtil.gbcSet(gbc, 1, 6, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(maxPerLayerChoice_, gbc);
    
  
    return (cp);
  }  
  
  /***************************************************************************
  **
  ** Build tab for links
  */ 
  
  private JPanel buildLinksTab(LayoutOptions options) { 
    ResourceManager rMan = appState_.getRMan();
    JPanel cp = new JPanel();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
   
    Vector<Integer> choices = new Vector<Integer>();
    for (int i = 0; i < LayoutOptions.MAX_OPT_PASSES; i++) {
      choices.add(new Integer(i));
    }
    optNumberChoice_ = new JComboBox(choices);
    optNumberChoice_.setSelectedItem(new Integer(options.optimizationPasses));
    JLabel label = new JLabel(rMan.getString("layoutParam.optPasses"));
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);       
    cp.add(label, gbc);    
    UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(optNumberChoice_, gbc);    
    
    //
    // Build a slider for each parameter:
    //

    String ccMin = rMan.getString("layoutParam.crossingCoeffMin");
    String ccMax = rMan.getString("layoutParam.crossingCoeffMax");
    String cmMin = rMan.getString("layoutParam.crossingMultMin");
    String cmMax = rMan.getString("layoutParam.crossingMultMax");
    String dcMin = rMan.getString("layoutParam.differenceCoeffMin");
    String dcMax = rMan.getString("layoutParam.differenceCoeffMax");    
    String dsMin = rMan.getString("layoutParam.differenceSigmaMin");    
    String dsMax = rMan.getString("layoutParam.differenceSigmaMax");    
       
    crossingCoeff_ = buildSlider(myCrossingCoeff_, CROSSING_COEFF_MIN_, CROSSING_COEFF_MAX_, ccMin, ccMax);
    crossingMultiplier_ = buildSlider(myCrossingMultiplier_, CROSSING_MULT_MIN_, CROSSING_MULT_MAX_, cmMin, cmMax);
    differenceCoeff_ = buildSlider(myDifferenceCoeff_, DIFF_COEFF_MIN_, DIFF_COEFF_MAX_, dcMin, dcMax);
    differenceSigma_ = buildSlider(myDifferenceSigma_, DIFF_SIGMA_MIN_, DIFF_SIGMA_MAX_, dsMin, dsMax);    

    String ccLabel = rMan.getString("layoutParam.crossingCoeff");
    String cmLabel = rMan.getString("layoutParam.crossingMult");
    String dcLabel = rMan.getString("layoutParam.differenceCoeff");
    String dsLabel = rMan.getString("layoutParam.differenceSigma");    
    
    label = new JLabel(ccLabel);
    UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, 1, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(crossingCoeff_, gbc); 
    
    label = new JLabel(cmLabel);
    UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, 2, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(crossingMultiplier_, gbc); 
    
    label = new JLabel(dcLabel);
    UiUtil.gbcSet(gbc, 0, 3, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, 3, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(differenceCoeff_, gbc); 
    
    label = new JLabel(dsLabel);
    UiUtil.gbcSet(gbc, 0, 4, 1, 1, UiUtil.NONE, 0, 0, 5, 5, 5, 5, UiUtil.E, 0.0, 1.0);
    cp.add(label, gbc);
    UiUtil.gbcSet(gbc, 1, 4, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    cp.add(differenceSigma_, gbc);     
    return (cp);
  }  
  
  /***************************************************************************
  **
  ** Build tab for propagation layout params 
  */ 
  
  private JPanel buildPropagateTab(LayoutOptions options) { 
    ResourceManager rMan = appState_.getRMan();
    JPanel cp = new JPanel();
    cp.setBorder(new EmptyBorder(20, 20, 20, 20));
    cp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
   
    inheritanceSquashButton_ = new JRadioButton(rMan.getString("layoutParam.inheritSquash"), options.inheritanceSquash);     
    UiUtil.gbcSet(gbc, 0, 0, 2, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 0.0);
    cp.add(inheritanceSquashButton_, gbc);
    
    JLabel overlayLabel = new JLabel(rMan.getString("layoutParam.overlayOptions"));
    Vector<ChoiceContent> relayoutChoices = NetOverlayProperties.getRelayoutOptions(appState_);
    overlayOptionCombo_ = new JComboBox(relayoutChoices);
    overlayOptionCombo_.setSelectedItem(NetOverlayProperties.relayoutForCombo(appState_, options.overlayOption));    
    
    UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(overlayLabel, gbc);
    UiUtil.gbcSet(gbc, 1, 1, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    cp.add(overlayOptionCombo_, gbc);
  
    return (cp);
  }  
  
  /***************************************************************************
  **
  ** Build tab for freshlayout 
  */ 
  
  private JPanel buildFreshLayoutTab(LayoutOptionsManager lom) {
    ResourceManager rMan = appState_.getRMan();
    JPanel retval = new JPanel();
    retval.setBorder(new EmptyBorder(20, 20, 20, 20));
    retval.setLayout(new GridLayout(1, 1));  
    JTabbedPane tabPane = new JTabbedPane();
    retval.add(tabPane);

    stackedParamPanel_ = new StackedBlockLayoutSetupPanel(appState_, null, false, lom.getStackedBlockLayoutParams(), true);  
    tabPane.addTab(rMan.getString("layoutParam.stacked"), stackedParamPanel_); 
 
    worksheetParamPanel_ = new WorksheetLayoutSetupPanel(appState_, null, false, false, lom.getWorksheetLayoutParams()); 
    tabPane.addTab(rMan.getString("layoutParam.worksheet"), worksheetParamPanel_); 

    worksheetDiagParamPanel_ = new WorksheetLayoutSetupPanel(appState_, null, false, true, lom.getDiagLayoutParams());  
    tabPane.addTab(rMan.getString("layoutParam.diagonal"), worksheetDiagParamPanel_); 
 
    haloParamPanel_ = new HaloLayoutSetupPanel(appState_, null, null, null, lom.getHaloLayoutParams()); 
    tabPane.addTab(rMan.getString("layoutParam.halo"), haloParamPanel_); 

    haloParamPanel_.displayProperties();
    worksheetParamPanel_.displayProperties();
    worksheetDiagParamPanel_.displayProperties();
    stackedParamPanel_.displayProperties();
    
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation.  If they have an error, pop
  ** up a warning dialog and return false, else return true.
  ** 
  */
 
  protected boolean stashForOK() {
    stashLegacyFieldsResults();
    if (!stackedParamPanel_.stashResults(true)) {
      return (false);
    }
    if (!worksheetParamPanel_.stashResults(true)) {
      return (false);
    }
    if (!worksheetDiagParamPanel_.stashResults(true)) {
      return (false);
    }     
    if (!haloParamPanel_.stashResults(true)) {
      return (false);
    }
    return (true);
  } 
  
  /***************************************************************************
  **
  ** Reset to default values
  ** 
  */
  
  private void resetDefaults() {
    resetLegacyFieldsDefaults();
    haloParamPanel_.resetDefaults();
    worksheetParamPanel_.resetDefaults();
    worksheetDiagParamPanel_.resetDefaults();
    stackedParamPanel_.resetDefaults();
    return;
  }   

  /***************************************************************************
  **
  ** Reset to default values
  ** 
  */
  
  private void resetLegacyFieldsDefaults() {
    LayoutOptions defOptions = new LayoutOptions();
    LinkPlacementGrid.GoodnessParams newParams = defOptions.goodness;
    int newCrossingCoeff = (int)Math.round(newParams.crossingCoeff / CROSSING_COEFF_CONVERT_);
    int newCrossingMultiplier = (int)Math.round(newParams.crossingMultiplier / CROSSING_MULT_CONVERT_);
    int newDifferenceCoeff = (int)Math.round(newParams.differenceCoeff / DIFF_COEFF_CONVERT_);
    int newDifferenceSigma = (int)Math.round(newParams.differenceSigma);
    crossingCoeff_.setValue(newCrossingCoeff);
    crossingMultiplier_.setValue(newCrossingMultiplier);
    differenceCoeff_.setValue(newDifferenceCoeff);
    differenceSigma_.setValue(newDifferenceSigma);

    myFirstPass_ = defOptions.firstPass; 
    myTopoCompress_ = defOptions.topoCompress;
    myLayerMethod_ = defOptions.layeringMethod; 
    myMaxPerLayer_ = defOptions.maxPerLayer;
    myDoCrossingReduction_ = defOptions.doCrossingReduction;
    myNormalizeRows_ = defOptions.normalizeRows;
    myIncrementalCompress_ = defOptions.incrementalCompress;
    myInheritanceSquash_ = defOptions.inheritanceSquash;    
    myOptNumber_ = defOptions.optimizationPasses;
    myOverlayOption_ = defOptions.overlayOption;
    firstPassButton_.setSelected(myFirstPass_);
    topoCompressButton_.setSelected(myTopoCompress_);
    layerMethodChoice_.setSelectedItem(LayoutOptions.mapLayerOptions(appState_, myLayerMethod_));
    maxPerLayerChoice_.setSelectedItem(new Integer(myMaxPerLayer_));
    optNumberChoice_.setSelectedItem(new Integer(myOptNumber_));    
    doCrossingReductionButton_.setSelected(myDoCrossingReduction_);
    normalizeRowsButton_.setSelected(myNormalizeRows_);
    incrementalCompressButton_.setSelected(myIncrementalCompress_);
    inheritanceSquashButton_.setSelected(myInheritanceSquash_);
    overlayOptionCombo_.setSelectedItem(NetOverlayProperties.relayoutForCombo(appState_, myOverlayOption_));
    return;
  }  

  /***************************************************************************
  **
  ** Stash incremental results for later interrogation. 
  ** 
  */
  
  private void stashLegacyFieldsResults() {
    myCrossingCoeff_  = crossingCoeff_.getValue();
    myCrossingMultiplier_ = crossingMultiplier_.getValue();
    myDifferenceCoeff_  = differenceCoeff_.getValue();
    myDifferenceSigma_ = differenceSigma_.getValue();
    myFirstPass_ = firstPassButton_.isSelected();
    myTopoCompress_ = topoCompressButton_.isSelected();
    myLayerMethod_ = ((ChoiceContent)layerMethodChoice_.getSelectedItem()).val;
    myMaxPerLayer_ = ((Integer)maxPerLayerChoice_.getSelectedItem()).intValue();
    myOptNumber_ = ((Integer)optNumberChoice_.getSelectedItem()).intValue();      
    myDoCrossingReduction_ = doCrossingReductionButton_.isSelected();
    myNormalizeRows_ = normalizeRowsButton_.isSelected();
    myIncrementalCompress_ = incrementalCompressButton_.isSelected();  
    myInheritanceSquash_ = inheritanceSquashButton_.isSelected();   
    myOverlayOption_ = ((ChoiceContent)overlayOptionCombo_.getSelectedItem()).val;
    return;
  }
  
  /***************************************************************************
  **
  ** Provide result
  ** 
  */
  
  private LayoutOptions getLegacyFieldsOptions() {
    LayoutOptions retval = new LayoutOptions();
    LinkPlacementGrid.GoodnessParams params = retval.goodness;
    params.crossingCoeff = (double)myCrossingCoeff_ * CROSSING_COEFF_CONVERT_;
    params.crossingMultiplier = (double)myCrossingMultiplier_ * CROSSING_MULT_CONVERT_;
    params.differenceCoeff = (double)myDifferenceCoeff_ * DIFF_COEFF_CONVERT_;
    params.differenceSigma = (double)myDifferenceSigma_;
    retval.firstPass = myFirstPass_;
    retval.topoCompress = myTopoCompress_;
    retval.layeringMethod = myLayerMethod_;
    retval.maxPerLayer = myMaxPerLayer_;
    retval.doCrossingReduction = myDoCrossingReduction_;
    retval.normalizeRows = myNormalizeRows_;
    retval.incrementalCompress = myIncrementalCompress_;
    retval.inheritanceSquash = myInheritanceSquash_;
    retval.overlayOption = myOverlayOption_;
    retval.optimizationPasses = myOptNumber_;
    return (retval);
  }
  
  /***************************************************************************
  **
  ** Build a slider panel
  */
  
  private JSlider buildSlider(int param, int min, int max, String minLabel, String maxLabel) {   
    JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, min);
    slider.setMinorTickSpacing(1);
    slider.setMajorTickSpacing(10);
    Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
    labels.put(new Integer(min), new JLabel(minLabel));
    labels.put(new Integer(max), new JLabel(maxLabel));
    slider.setLabelTable(labels);    
    slider.setPaintLabels(true);
    slider.setValue(param);
    return (slider);
  }
}
