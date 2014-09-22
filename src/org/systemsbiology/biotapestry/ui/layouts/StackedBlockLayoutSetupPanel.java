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

package org.systemsbiology.biotapestry.ui.layouts;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.ChoiceContent;
import org.systemsbiology.biotapestry.util.EnumChoiceContent;

/****************************************************************************
**
** Panel for setting the properties for a stacked block layout
*/

public class StackedBlockLayoutSetupPanel extends JPanel implements SpecialtyLayoutSetupPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private boolean haveResult_;
  private StackedBlockLayout.StackedBlockLayoutParams params_;
  private JComboBox targTypeCombo_;
  private JLabel targTypeLabel_;
  private JLabel targSizeLabel_;
  private JTextField targSizeField_;
  private JComboBox colorTypeCombo_;
  private JCheckBox doBubblesBox_;
  private JCheckBox doColorCheckBox_;
  private JComboBox overlayOptionCombo_;
  private boolean forSubset_;
  private JComboBox compressTypeCombo_; 
  private JComboBox srcTypeCombo_;
  private JCheckBox separateTargetsBox_;
  private boolean isForGlobal_;
  private boolean maxDisable_;
  private boolean partialDisable_;
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
  
  public StackedBlockLayoutSetupPanel(BTState appState, Genome genome, boolean forSubset,
                                      StackedBlockLayout.StackedBlockLayoutParams params,
                                      boolean forGlobalManager) { 
    appState_ = appState;
    haveResult_ = false;
    forSubset_ = forSubset;
    params_ = params.clone(); 
    if (forSubset_) {
      params_.assignColorMethod = ColorTypes.KEEP_COLORS;
    }
    isForGlobal_ = forGlobalManager;
    maxDisable_ = false;
    partialDisable_ = false;
    buildParamPanel(genome);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Get the params
  ** 
  */
  
  public SpecialtyLayoutEngineParams getParams() {
    return (params_);
  }
  
  /***************************************************************************
  **
  ** Answer if we have a result
  ** 
  */
  
  public boolean haveResult() {
    return (haveResult_);
  }
  
  /***************************************************************************
  **
  ** Reset to default values
  ** 
  */
  
  public void resetDefaults() {
    params_ = StackedBlockLayout.getDefaultParams(forSubset_);
    displayProperties();
    return;
  }   
       
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  public void displayProperties() {    
    StackedBlockLayout.StackedBlockLayoutParams defaultParams = StackedBlockLayout.getDefaultParams(forSubset_);   
    srcTypeCombo_.setSelectedItem(params_.grouping.generateCombo(appState_));
    boolean srcOver = params_.grouping.srcPositioningOverrides();
    boolean trgOver = params_.targsBelow;
    
    boolean useTB = (srcOver) ? defaultParams.targsBelow : params_.targsBelow;
    separateTargetsBox_.setSelected(useTB);
    
    int useRS = (srcOver) ? defaultParams.rowSize : params_.rowSize;
    targSizeField_.setText(Integer.toString(useRS));

    StackedBlockLayout.TargTypes useTG = (srcOver || trgOver) ? defaultParams.targGrouping : params_.targGrouping;
    targTypeCombo_.setSelectedItem(useTG.generateCombo(appState_));
   
    compressTypeCombo_.setSelectedItem(params_.compressType.generateCombo(appState_));
  
    if (colorTypeCombo_ != null) {
      colorTypeCombo_.setSelectedItem(params_.assignColorMethod.generateCombo(appState_));
      doBubblesBox_.setSelected(params_.showBubbles);
      doColorCheckBox_.setSelected(params_.checkColorOverlap);
    }
    overlayOptionCombo_.setSelectedItem(NetOverlayProperties.relayoutForCombo(appState_, params_.overlayOption));
    return;
  }
  
  /***************************************************************************
  **
  ** Stash our results for later interrogation.  If they have an error, pop
  ** up a warning dialog and return false, else return true.
  ** 
  */
  
  public boolean stashResults(boolean ok) {
    if (ok) {
      StackedBlockLayout.StackedBlockLayoutParams defaultParams = StackedBlockLayout.getDefaultParams(forSubset_);   
      boolean srcOver = params_.grouping.srcPositioningOverrides();
      boolean trgOver = params_.targsBelow;   
      
      if (!srcOver) {
        String trgStr = targSizeField_.getText();      
        boolean badVal = false;
        try {
          params_.rowSize = Integer.parseInt(trgStr);
          if (params_.rowSize <= 0) {
            badVal = true;
          }
        } catch (NumberFormatException ex) {
          badVal = true;
        }

        if (badVal) {
          ResourceManager rMan = appState_.getRMan();
          JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                        rMan.getString("stackedBlockLayout.badSize"), 
                                        rMan.getString("stackedBlockLayout.badSizeTitle"),
                                        JOptionPane.ERROR_MESSAGE);
          haveResult_ = false;
          return (false);
        }
      } else {
        params_.rowSize = defaultParams.rowSize;
      }
      
      if (colorTypeCombo_ != null) {
        params_.showBubbles = doBubblesBox_.isSelected();
        params_.checkColorOverlap = doColorCheckBox_.isSelected();
      }
      
      StackedBlockLayout.TargTypes altVal = (srcOver && !trgOver) ? defaultParams.targGrouping : null;
      handleUnchecked(params_, altVal);
      haveResult_ = true;
      return (true);
    } else {
      params_ = null;
      haveResult_ = false;
      return (true);
    }
  }
  
  /***************************************************************************
  **
  ** Bundle up unchecked extractions
  */
   
  @SuppressWarnings("unchecked")
  private void handleUnchecked(StackedBlockLayout.StackedBlockLayoutParams params, StackedBlockLayout.TargTypes altVal) {
    params.grouping = ((EnumChoiceContent<StackedBlockLayout.SrcTypes>)srcTypeCombo_.getSelectedItem()).val;
    params.targGrouping = (altVal != null) ? altVal : ((EnumChoiceContent<StackedBlockLayout.TargTypes>)targTypeCombo_.getSelectedItem()).val;
    params.overlayOption = ((ChoiceContent)overlayOptionCombo_.getSelectedItem()).val;
    params.compressType = ((EnumChoiceContent<StackedBlockLayout.CompressTypes>)compressTypeCombo_.getSelectedItem()).val;
    
    if (colorTypeCombo_ != null) {
      params.assignColorMethod = ((EnumChoiceContent<ColorTypes>)colorTypeCombo_.getSelectedItem()).val;
    }
    
    return;
  } 
  
  /***************************************************************************
  **
  ** Handle enabled/disabled controls
  */ 
  
  void handleEnabled() {
    separateTargetsBox_.setEnabled(!maxDisable_);
    targSizeLabel_.setEnabled(!maxDisable_);
    targSizeField_.setEnabled(!maxDisable_);
    targTypeCombo_.setEnabled(!maxDisable_ && !partialDisable_);
    targTypeLabel_.setEnabled(!maxDisable_ && !partialDisable_);
    repaint();
    return;
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
  ** Build it
  */ 
  
  private void buildParamPanel(Genome genome) {     
    ResourceManager rMan = appState_.getRMan();
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(new GridBagLayout());
    int rowNum = 0;
    
    JLabel srcTypeLabel = new JLabel(rMan.getString("stackedBlockLayout.srcType"));
    srcTypeCombo_ = new JComboBox(StackedBlockLayout.SrcTypes.getChoices(appState_, isForGlobal_));
        
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(srcTypeLabel, gbc);
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(srcTypeCombo_, gbc); 
    srcTypeCombo_.addActionListener(new ActionListener() {     
      @SuppressWarnings("unchecked")
      public void actionPerformed(ActionEvent ev) {
        try {
          EnumChoiceContent<StackedBlockLayout.SrcTypes> cc = (EnumChoiceContent<StackedBlockLayout.SrcTypes>)srcTypeCombo_.getSelectedItem();
          maxDisable_ = cc.val.srcPositioningOverrides();
          handleEnabled();
          return;
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });
    
    targSizeLabel_ = new JLabel(rMan.getString("stackedBlockLayout.targSize"));
    targSizeField_ = new JTextField();
    
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(targSizeLabel_, gbc);
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(targSizeField_, gbc);
       
    separateTargetsBox_ = new JCheckBox(rMan.getString("stackedBlockLayout.separateTargets"));
    UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(separateTargetsBox_, gbc); 
    separateTargetsBox_.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        try {
          partialDisable_ = !separateTargetsBox_.isSelected();
          handleEnabled();
          return;
        } catch (Exception ex) {
          appState_.getExceptionHandler().displayException(ex);
        }
      }
    });        
     
    targTypeLabel_ = new JLabel(rMan.getString("stackedBlockLayout.targType"));
    targTypeCombo_ = new JComboBox(StackedBlockLayout.TargTypes.getChoices(appState_, forSubset_));
        
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(targTypeLabel_, gbc);
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(targTypeCombo_, gbc);    
       
    JLabel compressTypeLabel = new JLabel(rMan.getString("stackedBlockLayout.compressType"));
    compressTypeCombo_ = new JComboBox(StackedBlockLayout.CompressTypes.getChoices(appState_, isForGlobal_));
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(compressTypeLabel, gbc);
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(compressTypeCombo_, gbc);    
       
    JLabel overlayLabel = new JLabel(rMan.getString("stackedBlockLayout.overlayOptions"));
    Vector<ChoiceContent> relayoutChoices = NetOverlayProperties.getRelayoutOptions(appState_);
    overlayOptionCombo_ = new JComboBox(relayoutChoices);
        
    boolean activate = (genome == null) || (genome.getNetworkOverlayCount() > 0);
    if (!activate) {
      overlayLabel.setEnabled(false);
      overlayOptionCombo_.setEnabled(false);
    }   
    
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(overlayLabel, gbc);
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(overlayOptionCombo_, gbc);  
    
    if (!forSubset_) {   
      JLabel colorTypeLabel = new JLabel(rMan.getString("specialtyLayout.colorStrategy"));
      colorTypeCombo_ = new JComboBox(ColorTypes.getChoices(appState_));
      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
      add(colorTypeLabel, gbc);
      UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
      add(colorTypeCombo_, gbc);    
      doBubblesBox_ = new JCheckBox(rMan.getString("specialtyLayout.doBubbles"));
      UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
      add(doBubblesBox_, gbc); 
      doColorCheckBox_ = new JCheckBox(rMan.getString("specialtyLayout.checkColors"));
      UiUtil.gbcSet(gbc, 0, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
      add(doColorCheckBox_, gbc);     
    }
 
    return;
  } 
}
