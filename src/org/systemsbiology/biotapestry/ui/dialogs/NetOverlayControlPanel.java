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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.systemsbiology.biotapestry.app.DynamicDataAccessContext;
import org.systemsbiology.biotapestry.app.UIComponentSource;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleAlphaBuilder;
import org.systemsbiology.biotapestry.ui.freerender.NetModuleFree;
import org.systemsbiology.biotapestry.util.ResourceManager;
import org.systemsbiology.biotapestry.util.UiUtil;

/****************************************************************************
**
** Panel for controlling display of a Network Overlay
*/

public class NetOverlayControlPanel extends JPanel implements ChangeListener {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  //////////////////////////////////////////////////////////////////////////// 
  
  private JSlider modSlider_;
  private ArrayList<JLabel> labels_;
  
  private boolean managing_;
  private NetModuleAlphaBuilder alphaCalc_;
  private DynamicDataAccessContext dacx_;
  private UIComponentSource uics_;
  
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
  
  public NetOverlayControlPanel(UIComponentSource uics, NetModuleAlphaBuilder alphaCalc, DynamicDataAccessContext dacx) {
    uics_ = uics;
    managing_ = false;
    alphaCalc_ = alphaCalc;
    dacx_ = dacx;
       
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    labels_ = new ArrayList<JLabel>();
    modSlider_ = buildSlider(labels_);
     
    UiUtil.gbcSet(gbc, 0, 0, 1, 1, UiUtil.HOR, 0, 0, 5, 5, 5, 5, UiUtil.W, 1.0, 1.0);
    add(modSlider_, gbc);
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  /***************************************************************************
  **
  ** Apply the current data values to our UI components
  ** 
  */
  
  public void displayProperties() {
    return;
  }
 
  /***************************************************************************
  **
  ** Use for progammatic setting of slider.  Note this should trigger
  ** the stateChanged operation...
  */
 
  public void setSliderValue(int slideVal) {
    modSlider_.setValue(slideVal);
    return;
  }  
  
  /***************************************************************************
  **
  ** Handle enabling
  ** 
  */
  
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    modSlider_.setEnabled(enabled);
    for (int i = 0; i < labels_.size(); i++) {
      JLabel label = labels_.get(i);
      label.setEnabled(enabled);
    }
    return;
  }   
  
  /***************************************************************************
  **
  ** Called when the slider moves
  */
 
  public void stateChanged(ChangeEvent e) {
    try {
      if (managing_) {
        return;
      }
      JSlider slider = (JSlider)e.getSource();
      int value = slider.getValue();
      NetModuleFree.CurrentSettings currSettings = dacx_.getOSO().getCurrentOverlaySettings();
      alphaCalc_.alphaCalc(value, currSettings);
      boolean maybeMasking = alphaCalc_.modContentsMasked(currSettings);
      uics_.getNetOverlayController().checkMaskState(maybeMasking, dacx_);
      uics_.getSUPanel().drawModel(false);
    } catch (Exception ex) {
      uics_.getExceptionHandler().displayException(ex);
    }
    return;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE METHODS
  //
  ////////////////////////////////////////////////////////////////////////////


  /***************************************************************************
  **
  ** Build a slider panel
  */
  
  private JSlider buildSlider(List<JLabel> labels) {
    JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
    ResourceManager rMan = dacx_.getRMan();
    Hashtable<Integer, JLabel> labelMap = new Hashtable<Integer, JLabel>();
    JLabel minLabel = new JLabel(rMan.getString("netOverlayController.min"));
    JLabel maxLabel = new JLabel(rMan.getString("netOverlayController.max"));
    labelMap.put(new Integer(0), minLabel);
    labels.add(minLabel);
    labelMap.put(new Integer(100), maxLabel);
    labels.add(maxLabel);
    slider.setLabelTable(labelMap); 
    slider.setMajorTickSpacing(100);    
    slider.setPaintTicks(true);
    slider.setPaintLabels(true);
    slider.addChangeListener(this);
    return (slider);
  } 
}
