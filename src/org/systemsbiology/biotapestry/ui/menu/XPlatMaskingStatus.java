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


package org.systemsbiology.biotapestry.ui.menu;

import java.util.HashSet;
import java.util.Set;

/****************************************************************************
**
** Provides info on the current toggle status (masking of actions during mouse input operations).
*/

public class XPlatMaskingStatus {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  private HashSet<String> maskedToOff_;
  private HashSet<String> maskedToOn_;
  private boolean noMasking_;
  
  private Boolean recentMenuOn_;
  private Boolean pathControlsOn_;
  private Boolean mainOverlayControlsOn_;
  private Boolean overlayLevelOn_;
  private Boolean keystrokesOn_;
  private Boolean modelDisplayOn_;
  private Boolean modelTreeOn_;
  private Boolean sliderOn_;
  private Boolean geeseOn_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////    

  /***************************************************************************
  **
  ** Constructor 
  */ 
  
  public XPlatMaskingStatus() { 
    maskedToOff_ = new HashSet<String>();
    maskedToOn_ = new HashSet<String>();
    noMasking_ = true;
  }
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
 
  /***************************************************************************
  **
  ** Ask if masking is active. If not, don't bother calling anything else, they will 
  ** just return null
  */ 
   
  public boolean isMaskingActive() {
    return (!noMasking_);
  }

  /***************************************************************************
  **
  ** Add to items that are masked to off
  */ 
   
  public void maskOff(String key) {
    noMasking_ = false;
    maskedToOff_.add(key);
    return;
  }
  
  /***************************************************************************
  **
  ** Add to items that are masked to on
  */ 
   
  public void maskOn(String key) {
    noMasking_ = false;
    maskedToOn_.add(key);
    return;
  }
  
  /***************************************************************************
  **
  ** Report items that are masked to off
  */ 
   
  public Set<String> getMaskedOff() {
    return (maskedToOff_);
  }
  
  /***************************************************************************
  **
  ** Report items that are masked to on
  */ 
   
  public Set<String> getMaskedOn() {
    return (maskedToOn_);
  }
  
  /***************************************************************************
  **
  ** Is recent menu active
  */ 
   
  public Boolean isRecentMenuOn() {
    return (recentMenuOn_);
  }
  
  /***************************************************************************
  **
  ** Set if recent menu active
  */ 
   
  public void setRecentMenuOn(boolean isOn) {
    noMasking_ = false;
    recentMenuOn_ = Boolean.valueOf(isOn);
    return;
  }
   
  /***************************************************************************
  **
  ** Are Path Controls active
  */ 
   
  public Boolean arePathControlsOn() {
    return (pathControlsOn_);
  }
  
  /***************************************************************************
  **
  ** Set if Path Controls active
  */ 
   
  public void setPathControlsOn(boolean isOn) {
    noMasking_ = false;
    pathControlsOn_ = Boolean.valueOf(isOn);
    return;
  }

  /***************************************************************************
  **
  ** Are Main Overlay Controls active
  */ 
   
  public Boolean areMainOverlayControlsOn() {
    noMasking_ = false;
    return (mainOverlayControlsOn_);
  }
  
  /***************************************************************************
  **
  ** Set if Main Overlay Controls active
  */ 
   
  public void setMainOverlayControlsOn(boolean isOn) {
    noMasking_ = false;
    mainOverlayControlsOn_ = Boolean.valueOf(isOn);
    return;
  }
  
  /***************************************************************************
  **
  ** Is overlay level active
  */ 
   
  public Boolean isOverlayLevelOn() {
    return (overlayLevelOn_);
  }  
  
  /***************************************************************************
  **
  ** Set overlay level active
  */ 
   
  public void setOverlayLevelOn(boolean isOn) {
    noMasking_ = false;
    overlayLevelOn_ = Boolean.valueOf(isOn);
    return;
  }  
  
  /***************************************************************************
  **
  ** Are keystrokes enabled
  */ 
   
  public Boolean areKeystrokesOn() {
    return (keystrokesOn_);
  }  
  
  /***************************************************************************
  **
  ** Set if keystrokes enabled
  */ 
   
  public void setKeystrokesOn(boolean isOn) {
    noMasking_ = false;
    keystrokesOn_ = Boolean.valueOf(isOn);
    return;
  }  
  
  /***************************************************************************
  **
  ** Is model display on
  */ 
   
  public Boolean isModelDisplayOn() {
    return (modelDisplayOn_);
  }  

  /***************************************************************************
  **
  ** Set model display on
  */ 
   
  public void setModelDisplayOn(boolean isOn) {
    noMasking_ = false;
    modelDisplayOn_ = Boolean.valueOf(isOn);
    return;
  }  
  
  /***************************************************************************
  **
  ** Is model tree on
  */ 
   
  public Boolean isModelTreeOn() {
    return (modelTreeOn_);
  }  
  
  /***************************************************************************
  **
  ** Set model tree on
  */ 
   
  public void setModelTreeOn(boolean isOn) {
    noMasking_ = false;
    modelTreeOn_ = Boolean.valueOf(isOn);
    return;
  }  

  /***************************************************************************
  **
  ** Is slider on
  */ 
   
  public Boolean isSliderOn() {
    return (sliderOn_);
  }  
  
  /***************************************************************************
  **
  ** Set if slider on
  */ 
   
  public void setSliderOn(boolean isOn) {
    noMasking_ = false;
    sliderOn_ = Boolean.valueOf(isOn);
    return;
  }  
  
  /***************************************************************************
  **
  ** Are geese on
  */ 
   
  public Boolean areGeeseOn() {
    return (geeseOn_);
  }   
   
  /***************************************************************************
  **
  ** Set are geese on
  */ 
   
  public void setGeeseOn(boolean isOn) {
    noMasking_ = false;
    geeseOn_ = Boolean.valueOf(isOn);
    return;
  }  
  
  /***************************************************************************
  **
  ** Set are geese on
  */ 
   
  @Override
  public String toString() {
    return (maskedToOff_ + " " +
            maskedToOn_ + " " +
            noMasking_ + " " +            
            recentMenuOn_ + " " +
            pathControlsOn_ + " " +
            mainOverlayControlsOn_ + " " +
            overlayLevelOn_ + " " +
            keystrokesOn_ + " " +
            modelDisplayOn_ + " " +
            modelTreeOn_ + " " +
            sliderOn_ + " " +
            geeseOn_);
  }  
}
