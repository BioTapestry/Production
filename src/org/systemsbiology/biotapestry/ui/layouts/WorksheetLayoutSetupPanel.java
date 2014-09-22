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
import org.systemsbiology.biotapestry.util.EnumChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.ChoiceContent;

/****************************************************************************
**
** Panel for setting the properties for a worksheet layout
*/

public class WorksheetLayoutSetupPanel extends JPanel implements SpecialtyLayoutSetupPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private boolean haveResult_;
  private WorksheetLayout.WorksheetLayoutParams params_;
  private JComboBox targTypeCombo_;
  private JComboBox srcTypeCombo_;
  private JTextField targSizeField_;
  private JTextField srcSizeField_;
  private JComboBox colorTypeCombo_;
  private JCheckBox doBubblesBox_;
  private JCheckBox doColorCheckBox_;
  private JComboBox overlayOptionCombo_;
  private BTState appState_;
  private boolean forSubset_;
  private boolean forDiagonal_;
 
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
  
  public WorksheetLayoutSetupPanel(BTState appState, Genome genome, boolean forSubset, 
                                   boolean forDiagonal, WorksheetLayout.WorksheetLayoutParams params) {     
    haveResult_ = false;
    appState_ = appState;
    forSubset_ = forSubset;
    forDiagonal_ = forDiagonal;
    params_ = params.clone();
    if (forSubset_) {
      params_.assignColorMethod = ColorTypes.KEEP_COLORS;
    }
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
    params_ = WorksheetLayout.getDefaultParams(forSubset_, forDiagonal_);
    displayProperties();
    return;
  }    
    
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  public void displayProperties() {
    
    if (srcTypeCombo_ != null) {
      srcTypeCombo_.setSelectedItem(params_.srcGroups.generateCombo(appState_));
      srcSizeField_.setText(Integer.toString(params_.srcSize));
    }
    targTypeCombo_.setSelectedItem(params_.targGroups.generateCombo(appState_));

    targSizeField_.setText(Integer.toString(params_.targSize));
 
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
      String srcStr = (!forDiagonal_) ? srcSizeField_.getText() : null;    
      String trgStr = targSizeField_.getText();      
      boolean badVal = false;
      try {
        if (!forDiagonal_) {
          params_.srcSize = Integer.parseInt(srcStr);
        }
        params_.targSize = Integer.parseInt(trgStr);
        if ((params_.srcSize <= 0) || (params_.targSize <= 0)) {
          badVal = true;
        }
      } catch (NumberFormatException ex) {
        badVal = true;
      }
      
      if (badVal) {
        ResourceManager rMan = appState_.getRMan();
        JOptionPane.showMessageDialog(appState_.getTopFrame(), 
                                      rMan.getString("worksheetLayout.badSize"), 
                                      rMan.getString("worksheetLayout.badSizeTitle"),
                                      JOptionPane.ERROR_MESSAGE);
        haveResult_ = false;
        return (false);
      }
      
      if (colorTypeCombo_ != null) {
        params_.showBubbles = doBubblesBox_.isSelected();
        params_.checkColorOverlap = doColorCheckBox_.isSelected();
      }       
      
      handleUnchecked(params_, forDiagonal_);
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
  private void handleUnchecked(WorksheetLayout.WorksheetLayoutParams params, boolean forDiagonal) {
    if (!forDiagonal) {
      params.srcGroups = ((EnumChoiceContent<WorksheetLayout.SrcTypes>)srcTypeCombo_.getSelectedItem()).val;
    }
    params.targGroups = ((EnumChoiceContent<WorksheetLayout.TargTypes>)targTypeCombo_.getSelectedItem()).val;
    params.overlayOption = ((ChoiceContent)overlayOptionCombo_.getSelectedItem()).val;
    
    if (colorTypeCombo_ != null) {
      params.assignColorMethod = ((EnumChoiceContent<ColorTypes>)colorTypeCombo_.getSelectedItem()).val;
    }
    
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
    
    if (!forDiagonal_) {
      JLabel srcTypeLabel = new JLabel(rMan.getString("worksheetLayout.srcType"));
      srcTypeCombo_ = new JComboBox(WorksheetLayout.SrcTypes.getChoices(appState_));

      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
      add(srcTypeLabel, gbc);
      UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
      add(srcTypeCombo_, gbc);

      JLabel srcSizeLabel = new JLabel(rMan.getString("worksheetLayout.srcSize"));
      srcSizeField_ = new JTextField();

      UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
      add(srcSizeLabel, gbc);
      UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
      add(srcSizeField_, gbc);
    }
    
    JLabel targTypeLabel = new JLabel(rMan.getString("worksheetLayout.targType"));
    targTypeCombo_ = new JComboBox(WorksheetLayout.TargTypes.getChoices(appState_));
    
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(targTypeLabel, gbc);
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(targTypeCombo_, gbc);    
       
    JLabel targSizeLabel = new JLabel(rMan.getString("worksheetLayout.targSize"));
    targSizeField_ = new JTextField();
    
    UiUtil.gbcSet(gbc, 0, rowNum, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(targSizeLabel, gbc);
    UiUtil.gbcSet(gbc, 1, rowNum++, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(targSizeField_, gbc);
 
    JLabel overlayLabel = new JLabel(rMan.getString("worksheetLayout.overlayOptions"));
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
