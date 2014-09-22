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
import java.util.Vector;

import org.systemsbiology.biotapestry.util.EnumChoiceContent;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.app.BTState;
import org.systemsbiology.biotapestry.genome.Genome;
import org.systemsbiology.biotapestry.ui.NetOverlayProperties;
import org.systemsbiology.biotapestry.util.ChoiceContent;


/****************************************************************************
**
** Dialog box for setting the properties for a worksheet layout
*/

public class HaloLayoutSetupPanel extends JPanel implements SpecialtyLayoutSetupPanel {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  private boolean haveResult_;
  private HaloLayout.HaloLayoutParams params_;
  private JComboBox strategyCombo_;
  private JComboBox overlayOptionCombo_;
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
  
  public HaloLayoutSetupPanel(BTState appState, Genome genome, 
                              String selectedID, HaloLayout halo, HaloLayout.HaloLayoutParams params) {
    appState_ = appState;
    haveResult_ = false;
    params_ = (HaloLayout.HaloLayoutParams)params.clone();
    params_.selected = selectedID;

    ResourceManager rMan = appState_.getRMan();
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    if ((params_.selected != null) && !halo.selectionIsValid(genome, params_.selected)) {
      params_.selected = null;
    }    

    JLabel strategyLabel = new JLabel(rMan.getString("haloLayout.strategyType"));
    strategyCombo_ = new JComboBox(HaloLayout.StartSeed.getChoices(appState_, params_.selected != null));
    
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(strategyLabel, gbc);
    UiUtil.gbcSet(gbc, 1, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(strategyCombo_, gbc);
    
    JLabel overlayLabel = new JLabel(rMan.getString("haloLayout.overlayOptions"));
    Vector<ChoiceContent> relayoutChoices = NetOverlayProperties.getRelayoutOptions(appState);
    overlayOptionCombo_ = new JComboBox(relayoutChoices);
    
    boolean activate = (genome == null) || (genome.getNetworkOverlayCount() > 0);
    if (!activate) {
      overlayLabel.setEnabled(false);
      overlayOptionCombo_.setEnabled(false);
    }
       
    UiUtil.gbcSet(gbc, 0, 1, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(overlayLabel, gbc);
    UiUtil.gbcSet(gbc, 1, 1, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.CEN, 1.0, 0.0);    
    add(overlayOptionCombo_, gbc);  

  }
  
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
    params_ = HaloLayout.getDefaultParams();
    displayProperties();
    return;
  }     
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  public void displayProperties() {
    strategyCombo_.setSelectedItem(params_.startType.generateCombo(appState_));
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
      handleUnchecked(params_);
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
  private void handleUnchecked(HaloLayout.HaloLayoutParams params) {    
    params.startType = ((EnumChoiceContent<HaloLayout.StartSeed>)strategyCombo_.getSelectedItem()).val;
    params.overlayOption = ((ChoiceContent)overlayOptionCombo_.getSelectedItem()).val;
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
  
}
