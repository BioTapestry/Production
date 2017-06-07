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

package org.systemsbiology.biotapestry.app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.systemsbiology.biotapestry.cmd.flow.FlowMeister;
import org.systemsbiology.biotapestry.cmd.flow.HarnessBuilder;
import org.systemsbiology.biotapestry.cmd.flow.modelTree.SetCurrentModel;
import org.systemsbiology.biotapestry.db.TimeAxisDefinition;
import org.systemsbiology.biotapestry.event.ModelChangeEvent;
import org.systemsbiology.biotapestry.genome.DynamicInstanceProxy;
import org.systemsbiology.biotapestry.util.UiUtil;
import org.systemsbiology.biotapestry.util.UndoFactory;

/****************************************************************************
**
** Wrapper around actual time slider implementation
*/

public class VirtualTimeSlider implements ChangeListener { 
                                                             
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  

  private JPanel sliderPanel_;
  private String currDip_;
  private int lastSetting_;
  private Integer frozenSlider_;
  private boolean managing_;
  private HashMap<String, JSlider> sliders_;
  private String currSlider_;
  private JPanel blankPanel_;
  private JPanel currSliderPanel_;
  private boolean sliderVisible_;
  private boolean doingUndo_;
  private boolean doNotFlow_;
  private HarnessBuilder hBld_;
  private DynamicDataAccessContext ddacx_;
  private UIComponentSource uics_;
  private TabSource tSrc_; 
  private UndoFactory uFac_;
   
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public VirtualTimeSlider(DynamicDataAccessContext ddacx, UIComponentSource uics, HarnessBuilder hBld, 
                           TabSource tSrc, UndoFactory uFac) {
    ddacx_ = ddacx;
    hBld_ = hBld;
    uics_ = uics;
    tSrc_ = tSrc;
    uFac_ = uFac;
    lastSetting_ = -1;
    sliders_ = new HashMap<String, JSlider>();
    doNotFlow_ = false;
    
    sliderPanel_ = new JPanel();
    sliderPanel_.setBorder(new LineBorder(Color.black, 1));
    sliderVisible_ = false;
    sliderPanel_.setMinimumSize(new Dimension(250,0));
    sliderPanel_.setPreferredSize(new Dimension(250,0));
    sliderPanel_.setLayout(new GridLayout());
    blankPanel_ = new JPanel();
    sliderPanel_.add(blankPanel_);   
  }
    
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Get the panel
  */ 

  public JPanel getSliderPanel() {
    return (sliderPanel_);
  }
 
  /***************************************************************************
  **
  ** Set the state
  */ 

  public void setSliderEnabled(boolean ena) {
    if (ena) {
      enableControls();
    } else {
      disableControls();
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Disable the main controls
  */ 
  
  private void disableControls() {
    if (currSlider_ != null) {
      JSlider slider = sliders_.get(currSlider_);
      if (slider != null) {
        slider.setEnabled(false);
      }
    }
    return;
  }  
 
  /***************************************************************************
  **
  ** Reenable the controls
  */ 
  
  private void enableControls() {
    if (currSlider_ != null) {
      JSlider slider = sliders_.get(currSlider_);
      if (slider != null) {
        slider.setEnabled(true);
      }
    } 
    return;
  }
 
  /***************************************************************************
  **
  ** Find the slider key
  */
  
  public String getCurrentSliderKey(DynamicInstanceProxy dip) { 
    JSlider slider = sliders_.get(dip.getID());
    int val;
    if (slider == null) {
      val = dip.getMinimumTime();
    } else {
      val = slider.getValue();
    }
    return (dip.getKeyForTime(val, true));
  }
  
  /***************************************************************************
  **
  ** Handle empty slider display
  */
   
  public void installEmptySlider() {
    sliderPanel_.remove(0);
    sliderPanel_.add(blankPanel_);
    sliderPanel_.revalidate();
    sliderPanel_.repaint();
    return; 
  }
  
  /***************************************************************************
  **
  ** Handle slider display for a proxied genome
  */
   
  public void manageSliderForProxy(String proxyID) {
    currDip_ = proxyID;
    DynamicInstanceProxy dip = ddacx_.getGenomeSource().getDynamicProxy(proxyID);
    manageSliderSetup(dip, null);
    sliderPanel_.remove(0);
    sliderPanel_.add(currSliderPanel_);      
    sliderPanel_.revalidate();
    sliderPanel_.repaint();   
    return; 
  }

  /***************************************************************************
  **
  ** Drop the slider when model is dropped
  */ 
  
  public void dropTheSlider() {
    sliders_.clear();
    return;
  }
  
  /***************************************************************************
  **
  ** Notify listener of model change
  */ 
  
  public String handleSliderModelChange(ModelChangeEvent mcev) {
    if (noLongerHourly(mcev)) {
      hideAndDitchSlider(mcev.getOldKey(), mcev.oldKeyIsProxy());
    } else if (newlyHourly(mcev)) {
      setupAndDisplayNewSlider(mcev.getProxyKey());
    } else if (hourlyChange(mcev)) {
      ditchOldAndDisplayNewSlider(mcev.getProxyKey());
    } 
    String id = null;
    if (mcev.isProxyKey()) {
      String proxID = mcev.getProxyKey();
      id = ddacx_.getGenomeSource().getDynamicProxy(proxID).getKeyForTime(lastSetting_, true);
    } else {
      id = mcev.getGenomeKey();
    }
    return (id);
  }
  
  /***************************************************************************
  **
  ** Ditch the slider for a model if it is no longer valid
  */
  
  public void ditchSliderIfStale(ModelChangeEvent mcev) {  
    String id = null;
    if (mcev.isProxyKey()) {
      id = mcev.getProxyKey();
    } else if (mcev.oldKeyIsProxy()) {
      id = mcev.getOldKey();
    } else {
      return;
    }
    JSlider slider = sliders_.get(id);
    if (slider != null) {    
      DynamicInstanceProxy dip = ddacx_.getGenomeSource().getDynamicProxy(id);
      int dipMin = dip.getMinimumTime();
      int dipMax = dip.getMaximumTime();
      if ((slider.getMinimum() != dipMin) || (slider.getMaximum() != dipMax)) {
        sliders_.remove(id);
      }
    }
    return;
  }
  
  /***************************************************************************
  **
  ** Answers if mode change is hourly->single
  */
  
  private boolean noLongerHourly(ModelChangeEvent mcev) {
    return ((mcev.getOldKey() != null) && mcev.oldKeyIsProxy() && (!mcev.isProxyKey()));
  }
  
  /***************************************************************************
  **
  ** Answers if mode change is single->hourly
  */
  
  private boolean newlyHourly(ModelChangeEvent mcev) {
    return ((mcev.getOldKey() != null) && (!mcev.oldKeyIsProxy()) && (mcev.isProxyKey()));
  }

  /***************************************************************************
  **
  ** Answers if mode change is hourly->hourly, with a different range
  */
  
  private boolean hourlyChange(ModelChangeEvent mcev) {
    if (!mcev.isProxyKey()) {
      return (false);
    }
    String id = mcev.getProxyKey();
    JSlider slider = sliders_.get(id);
    if (slider == null) {
      throw new IllegalStateException();
    }
    DynamicInstanceProxy dip = ddacx_.getGenomeSource().getDynamicProxy(id);
    int dipMin = dip.getMinimumTime();
    int dipMax = dip.getMaximumTime();
    return ((slider.getMinimum() != dipMin) || (slider.getMaximum() != dipMax));
  }  

  /***************************************************************************
  **
  ** Hide and throw away the now useless slider
  */
  
  public void hideAndDitchSlider(String oldKey, boolean oldKeyIsProxy) {
    if ((oldKey == null) || !oldKeyIsProxy) {
      throw new IllegalStateException();
    }
    JSlider slider = sliders_.get(oldKey);
    if (slider != null) {    
      sliders_.remove(oldKey);
    }
    sliderPanel_.remove(0);
    sliderPanel_.add(blankPanel_);
    sliderPanel_.revalidate();
    sliderPanel_.repaint();
    return;
  }
  
  /***************************************************************************
  **
  ** Create and display a newly required slider
  */
  
  public void setupAndDisplayNewSlider(String proxyKey) {
    Integer time = frozenSlider_;  
    DynamicInstanceProxy dip = ddacx_.getGenomeSource().getDynamicProxy(proxyKey);
    currDip_ = proxyKey;
    manageSliderSetup(dip, time);
    sliderPanel_.remove(0);
    sliderPanel_.add(currSliderPanel_);      
    sliderPanel_.revalidate();
    sliderPanel_.repaint();
    frozenSlider_ = null;
    return;
  }
  
  /***************************************************************************
  **
  ** Ditch old, create and display new slider
  */
  
  public void ditchOldAndDisplayNewSlider(String proxyKey) {
    Integer time = frozenSlider_;
    JSlider slider = sliders_.get(proxyKey);
    if (slider != null) {    
      sliders_.remove(proxyKey);
    }   
    DynamicInstanceProxy dip = ddacx_.getGenomeSource().getDynamicProxy(proxyKey);
    currDip_ = proxyKey;
    manageSliderSetup(dip, time);
    sliderPanel_.remove(0);
    sliderPanel_.add(currSliderPanel_);      
    sliderPanel_.revalidate();
    sliderPanel_.repaint(); 
    frozenSlider_ = null;
    return;
  }
  
  /***************************************************************************
  **
  ** Set if we should not launch our own control flow in our event handler
  */
  
  public void setDoNotFlow(boolean val) {
    doNotFlow_ = val;
    return;
  }  
  
  
  /***************************************************************************
  **
  ** Programatic slider change
  */
  
  public void setSliderForTime(DynamicInstanceProxy dip, int newTime) { 
    JSlider slider = sliders_.get(dip.getID());
    slider.setValue(newTime);
    return;
  }  
        
  /***************************************************************************
  **
  ** Undo a change
  */
  
  public void handleSliderUndo(NavigationChange undo) {
    if (undo.sliderID != null) {
      doingUndo_ = true;
      JSlider slider = sliders_.get(undo.sliderID);
      slider.setValue(undo.oldHour);
      doingUndo_ = false;
    }
    return;
  }  
  
  /***************************************************************************
  **
  ** Redo a change
  */
  
  public void handleSliderRedo(NavigationChange undo) {
    if (undo.sliderID != null) {
      doingUndo_ = true;
      JSlider slider = sliders_.get(undo.sliderID);
      slider.setValue(undo.newHour);
      doingUndo_ = false;
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
      if (!slider.getValueIsAdjusting()) {
        int value = slider.getValue();
        if (value == lastSetting_) {
          return;
        }
        if (doNotFlow_) {
          //
          // This means we are in the middle of another control flow execution, and we have no business launching another control 
          // flow.  So just use the guts of the switching operation from the SetCurrentModel.StepState flow state:
          //      
          SetCurrentModel.StepState agis = (SetCurrentModel.StepState)hBld_.getNewCmdState(FlowMeister.OtherFlowKey.MODEL_SELECTION_FROM_SLIDER, ddacx_);
          int lastSettingToUse = lastSetting_;
          lastSetting_ = value;
          agis.setPreloadForSlider(value, lastSettingToUse, doingUndo_, currDip_, uics_, tSrc_, uFac_);
          agis.stepToProcess();
        } else {
          HarnessBuilder.PreHarness pH = hBld_.buildHarness(FlowMeister.OtherFlowKey.MODEL_SELECTION_FROM_SLIDER);          
          SetCurrentModel.StepState agis = (SetCurrentModel.StepState)pH.getCmdState();  
          int lastSettingToUse = lastSetting_;
          lastSetting_ = value;
          agis.setPreloadForSlider(value, lastSettingToUse, doingUndo_, currDip_, uics_, tSrc_, uFac_);
          agis.setAppState(ddacx_.getMetabase().getAppState(), ddacx_); // UGH! FIXME
          hBld_.runHarness(pH);
        }
      }
    } catch (Exception ex) {
      uics_.getExceptionHandler().displayException(ex);
    }
    return;
  }

  /***************************************************************************
  **
  ** Build a slider panel
  */
  
  private JSlider buildSlider(DynamicInstanceProxy dip, Integer time) {
    int min = dip.getMinimumTime();
    int max = dip.getMaximumTime();    
    JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, min);
    slider.setMinorTickSpacing(1);
    int spacing = ((max - min) < 6) ? 1 : 3;
    slider.setMajorTickSpacing(spacing);
    TimeAxisDefinition tad = ddacx_.getExpDataSrc().getTimeAxisDefinition();
    if (tad.haveNamedStages()) {
      Hashtable<Integer, JLabel> namedStageMap = new Hashtable<Integer, JLabel>();
      List<TimeAxisDefinition.NamedStage> stages = tad.getNamedStages();
      int numStage = stages.size();
      for (int i = 0; i < numStage; i++) {
        String stageName = stages.get(i).abbrev;
        namedStageMap.put(new Integer(i), new JLabel(stageName));
      }
      slider.setLabelTable(namedStageMap); 
    } else {
      slider.setLabelTable(slider.createStandardLabels(spacing));            
    }
    slider.setPaintTicks(true);
    slider.setPaintLabels(true);
    slider.setSnapToTicks(true);
    if (time != null) {
      int timeVal = time.intValue();
      if ((timeVal >= min) && (timeVal <= max)) {
        slider.setValue(time.intValue());
      }
    }
    slider.addChangeListener(this);
    GridBagConstraints gbc = new GridBagConstraints();    
    
    currSliderPanel_ = new JPanel();
    currSliderPanel_.setLayout(new GridBagLayout());
    UiUtil.gbcSet(gbc, 0, 0, 1, 2, UiUtil.BO, 0, 0, 5, 5, 0, 5, UiUtil.N, 1.0, 1.0);
    currSliderPanel_.add(slider, gbc); 
    UiUtil.gbcSet(gbc, 0, 2, 1, 1, UiUtil.HOR, 0, 0, 0, 0, 5, 0, UiUtil.CEN, 1.0, 0.0);
    String displayUnits = tad.unitDisplayString();
    currSliderPanel_.add(new JLabel(displayUnits, JLabel.CENTER), gbc);
    return (slider);
  }
  
  /***************************************************************************
  **
  ** Handle undo recording for slider changes
  ** 
  */
  
  public SliderChange recordSliderStateForUndo() {
    frozenSlider_ = new Integer(lastSetting_);
    SliderChange retval = new SliderChange();
    retval.vtSlider = this;
    retval.timeVal = frozenSlider_;
    return (retval);
  } 
  
  /***************************************************************************
  **
  ** Handle undo restoration for slider changes
  ** 
  */
  
  public void restoreSliderState(Integer timeVal) {
    frozenSlider_ = timeVal;
    return;
  }   
   
  /***************************************************************************
  **
  ** Manages slider setup
  */
  
  private void manageSliderSetup(DynamicInstanceProxy dip, Integer time) { 
    managing_ = true;
    JSlider slider = sliders_.get(dip.getID());
    if (slider == null) {
      slider = buildSlider(dip, time);
      sliders_.put(dip.getID(), slider);
    } else {
      currSliderPanel_ = (JPanel)slider.getParent();
    }
    currSlider_ = dip.getID();    
    lastSetting_ = slider.getValue();
    managing_ = false;
    return;
  }  

  /***************************************************************************
  **
  ** Get last slider setting
  */
  
  public int getLastSliderSetting() { 
    return (lastSetting_);
  } 
  
  /***************************************************************************
  **
  ** Used to set the visibility of the slider controls
  */
  
  public void makeSliderVisible(boolean show) {
    if (sliderVisible_ == show) {
      return;
    }
    if (!uics_.isHeadless()) {
      int height = (show) ? 75 : 0;
      sliderPanel_.setMinimumSize(new Dimension(250, height));
      sliderPanel_.setPreferredSize(new Dimension(250, height));
      sliderPanel_.invalidate();
      uics_.getContentPane().validate();
    }
    sliderVisible_ = show;
    return;
  }    
}
